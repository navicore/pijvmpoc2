package onextent.iot.pijvmpoc2

import scala.concurrent.ExecutionContext.Implicits.global
import akka.Done
import com.pi4j.io.gpio.{GpioController, GpioFactory, RaspiGpioProvider, RaspiPinNumberingScheme}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.streams._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends LazyLogging {

  GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING))
  val gpio: GpioController = GpioFactory.getInstance()

  def main(args: Array[String]): Unit = {

    println("starting...")

    val done: Future[Done] = TempAndHumidityReporter2()

    done onComplete {
      case Success(s) =>
        gpio.shutdown()
        logger.debug(s"stream processing complete: $s")
      case Failure(e) =>
        gpio.shutdown()
        logger.error(s"stream failed on $e")
    }

  }

}
