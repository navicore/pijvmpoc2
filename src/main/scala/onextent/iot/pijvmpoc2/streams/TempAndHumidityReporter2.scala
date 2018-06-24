package onextent.iot.pijvmpoc2.streams

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.pi4j.io.gpio.RaspiBcmPin
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import onextent.iot.pijvmpoc2.Conf._
import onextent.iot.pijvmpoc2.io.{Dht22Sensor, SR04Sensor}
import onextent.iot.pijvmpoc2.models._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Akka Stream whose source is a temp and humidity module on a PI
  * and whose Sink reports to an https API
  */
object TempAndHumidityReporter2 extends LazyLogging {

  def createToConsumer(consumer: Sink[MqttMessage, Future[Done]])(
      implicit s: ActorSystem,
      m: Materializer): Sink[MqttMessage, NotUsed] = {
    val runnableGraph: RunnableGraph[Sink[MqttMessage, NotUsed]] =
      MergeHub
        .source[MqttMessage](perProducerBufferSize = 16)
        .to(consumer)
    runnableGraph.run()
  }

  def throttlingFlow[T]: Flow[T, T, NotUsed] =
    Flow[T].throttle(
      elements = 1,
      per = intervalSeconds,
      maximumBurst = 0,
      mode = ThrottleMode.Shaping
    )

  val connectionSettings: MqttConnectionSettings =
    MqttConnectionSettings(
      mqttUrl,
      mqttClientId,
      new MemoryPersistence
    ).withAuth(mqttUser, mqttPwd)

  val sinkSettings: MqttConnectionSettings =
    connectionSettings.withClientId(clientId = mqttClientId)

  def apply(): NotUsed = {

    def measureDistance() =
      (r: (Int, Command)) => (r._1, SR04Sensor())

    def read() =
      (r: (Int, Command)) => (r._1, Dht22Sensor(r._1))

    def distReadings() =
      (t: (Int, Option[UltraSonicReading])) => {
        t._2 match {
          case Some(reading) =>
            List(UltraSonicReport(Some(s"$deviceId-temp-${t._1}"), reading))
          case _ =>
            logger.warn(s"empty ultrasonic reading")
            List()
        }
      }

    def tempReadings =
      (t: (Int, Option[TempReading])) => {
        t._2 match {
          case Some(reading) =>
            List(TempReport(Some(s"$deviceId-temp-${t._1}"), reading))
          case _ =>
            logger.warn(s"empty temp reading")
            List()
        }
      }

    // read port 4 every n seconds
    val s1 = Source.fromGraph(new CommandSource(4)).via(throttlingFlow)

    // read temp port 4 when button is pressed
    val s2 = Source.fromGraph(new ButtonSource(RaspiBcmPin.GPIO_02, 4))

    val s3 = Source.fromGraph(new CommandSource(21)).via(throttlingFlow)

    val toConsumer = createToConsumer(
      MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    def mqttReading[T](implicit ioc: Encoder[T]) =
      (r: T) =>
        MqttMessage(mqttTopic,
                    ByteString(r.asJson.noSpaces),
                    Some(MqttQoS.AtLeastOnce),
                    retained = true)

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        s1
      }
      //Source
      //  .fromGraph(s1)
      .map(read())
      .mapConcat(tempReadings)
      .map(mqttReading)
      .runWith(toConsumer)

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        s2
      }
      //Source
      //  .fromGraph(s2)
      .map(read())
      .mapConcat(tempReadings)
      .map(mqttReading)
      .runWith(toConsumer)

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        s3
      }
      //Source
      //  .fromGraph(s3)
      .map(measureDistance())
      .mapConcat(distReadings())
      .map(mqttReading)
      .runWith(toConsumer)

  }

}
