package onextent.iot.pijvmpoc2

import onextent.iot.pijvmpoc2.io._
import onextent.iot.pijvmpoc2.streams.TempAndHumidityReporter

object Main {

  def main(args: Array[String]): Unit = {

    println("starting...")

    GpioBlinkExample()

    PwmLedExample()

    TempAndHumidityReporter()

  }

}
