package onextent.iot.pijvmpoc2.streams

import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl.{Flow, Merge, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._
import onextent.iot.pijvmpoc2.Conf._
import onextent.iot.pijvmpoc2.models.{TempReading, TempReport}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Akka Stream whose source is a temp and humidity module on a PI
  * and whose Sink reports to an https API
  */
object TempAndHumidityReporter extends LazyLogging {

  val throttlingFlow
    : Flow[(Int, Option[TempReading]), (Int, Option[TempReading]), NotUsed] =
    Flow[(Int, Option[TempReading])].throttle(
      elements = 1,
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
    connectionSettings.withClientId(clientId = "sink-spec/sink")

  def apply(): Future[Done] = {

    //todo: refactor:
    // make 2 sources
    //  1 - issue commands via throttlingFlow
    //  2 - issue commands via button
    // merge s1 and s2 as before
    // make 1 new Flow that runs the pwm light
    // make 1 new Flow that extracts the temp and humidity (model after tempReadings)
    // pass to tempReadings
    // pass to mqttReadings
    // sink to mqttSink


    val s1 = Source.fromGraph(new Dht22SensorSource(4)).via(throttlingFlow)

    //todo: make s2 be via button
    //val s2 = Source.fromGraph(new Dht22SensorSource(22)).via(throttlingFlow)

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
      .mapConcat(tempReadings())
      .map(mqttReading())
      .runWith(mqttSink)

  }

}
