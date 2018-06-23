package onextent.iot.pijvmpoc2

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.streams._

object Main extends LazyLogging {

  GpioFactory.setDefaultProvider(
    new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING))
  val gpio: GpioController = GpioFactory.getInstance()

  def main(args: Array[String]): Unit = {

    println("starting...")

    TempAndHumidityReporter2()

  }

}
