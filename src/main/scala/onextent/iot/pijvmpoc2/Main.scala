package onextent.iot.pijvmpoc2

import scala.concurrent.ExecutionContext.Implicits.global
import akka.Done
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.streams._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends LazyLogging {

  def main(args: Array[String]): Unit = {

    println("starting...")

    val done: Future[Done] = TempAndHumidityReporter2()

    done onComplete {
      case Success(s) => logger.debug(s"stream processing complete: $s")
      case Failure(e) => logger.error(s"stream failed on $e")
    }

  }

}
