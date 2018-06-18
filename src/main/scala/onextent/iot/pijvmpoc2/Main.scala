package onextent.iot.pijvmpoc2

import scala.concurrent.ExecutionContext.Implicits.global
import akka.Done
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.streams.TempAndHumidityReporter

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends LazyLogging {

  def main(args: Array[String]): Unit = {

    println("starting...")

    val done: Future[Done] = TempAndHumidityReporter()

    done onComplete {
      case Success(s) => logger.debug(s"stream got $s")
      case Failure(e) => logger.error(s"stream got $e")
    }

  }

}
