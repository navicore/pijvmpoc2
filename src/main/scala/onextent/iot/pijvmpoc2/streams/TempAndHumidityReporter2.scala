package onextent.iot.pijvmpoc2.streams

import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl.{Flow, Merge, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.pi4j.io.gpio.RaspiBcmPin
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._
import onextent.iot.pijvmpoc2.Conf._
import onextent.iot.pijvmpoc2.io.Dht22Sensor
import onextent.iot.pijvmpoc2.models.{Command, TempReading, TempReport}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Akka Stream whose source is a temp and humidity module on a PI
  * and whose Sink reports to an https API
  */
object TempAndHumidityReporter2 extends LazyLogging {

  def throttlingFlow[T]
    : Flow[T, T, NotUsed] =
    Flow[T].throttle(
      elements = 1,
      //per = intervalSeconds,
      per = 30.seconds,
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

  def apply(): Future[Done] = {

    def read() =
      (r: (Int, Command)) => (r._1, Dht22Sensor(r._1))

    // read port 4 every n seconds
    val s1 = Source.fromGraph(new CommandSource(4)).via(throttlingFlow)

    // BUG! when both s1 and s2 are wired s1 doesn't work
    // BUG! when both s1 and s2 are wired s1 doesn't work
    // BUG! when both s1 and s2 are wired s1 doesn't work
    // BUG! when both s1 and s2 are wired s1 doesn't work
    // BUG! when both s1 and s2 are wired s1 doesn't work
    // BUG! when both s1 and s2 are wired s1 doesn't work
    // ... but they work perfectly by themselves

    // read temp port 4 when button is pressed
    //val s2 = Source.fromGraph(new ButtonSource(RaspiBcmPin.GPIO_02, 4))

    val mqttSink = MqttSink(sinkSettings, MqttQoS.atLeastOnce)

    def tempReadings() =
      (t: (Int, Option[TempReading])) => {
        t._2 match {
          case Some(reading) =>
            List(TempReport(Some(s"$deviceId-temp-${t._1}"), reading))
          case _ =>
            List()
        }
      }

    def mqttReading() =
      (r: TempReport) =>
        MqttMessage(mqttTopic,
                    ByteString(r.asJson.noSpaces),
                    Some(MqttQoS.AtLeastOnce),
                    retained = true)

    Source
      .fromGraph(s1)
      //.combine(s1, s2)(Merge(_))
      .map(read())
      .mapConcat(tempReadings())
      .map(mqttReading())
      .runWith(mqttSink)

  }

}
