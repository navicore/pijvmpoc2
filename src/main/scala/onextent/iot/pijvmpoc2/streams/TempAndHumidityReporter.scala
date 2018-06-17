package onextent.iot.pijvmpoc2.streams

import scala.concurrent.ExecutionContext.Implicits.global
import akka.{Done, NotUsed}
import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl.{Flow, Merge, Sink, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._
import onextent.iot.pijvmpoc2.Conf._
import onextent.iot.pijvmpoc2.models.{TempReading, TempReport}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Akka Stream whose source is a temp and humidity module on a PI
  * and whose Sink reports to an https API
  */
object TempAndHumidityReporter extends LazyLogging {

  def apply(): Unit = {

    val throttlingFlow = Flow[(Int, Option[TempReading])].throttle(
      elements = 1,
      per = 30.seconds,
      maximumBurst = 0,
      mode = ThrottleMode.Shaping
    )

    val s1 = Source.fromGraph(new Dht22SensorSource(4)).via(throttlingFlow)

    val s2 = Source.fromGraph(new Dht22SensorSource(22)).via(throttlingFlow)

    val connectionSettings =
      MqttConnectionSettings(
        mqttUrl,
        mqttClientId,
        new MemoryPersistence
      ).withAuth(mqttUser, mqttPwd)

    val sinkSettings = connectionSettings.withClientId(clientId = "sink-spec/sink")

    val mqttSink = MqttSink(sinkSettings, MqttQoS.atLeastOnce)

    def tempReadings() =
      (t: (Int, Option[TempReading])) => {
        t._2 match {
          case Some(reading) =>
            List(TempReport(Some(s"navisensor-${t._1}"), reading))
          case _ =>
            List()
        }
      }

    def mqttReading() =
      (r: TempReport) => MqttMessage(mqttTopic, ByteString(r.asJson.noSpaces), Some(MqttQoS.AtLeastOnce), retained = true)

    val done: Future[Done] = Source
      .combine(s1, s2)(Merge(_))
      .mapConcat(tempReadings())
      .map(mqttReading())
      .runWith(mqttSink)

    done onComplete {
      case Success(s) => logger.debug(s"stream got $s")
      case Failure(e) => logger.error(s"stream got $e")
    }

  }

}
