package onextent.iot.pijvmpoc2.io

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.models.UltraSonicReading

object SR04Sensor extends LazyLogging {

  val gpio: GpioController = GpioFactory.getInstance()

  val trigPin: Pin = RaspiBcmPin.GPIO_20
  val triggerOuptut: GpioPinDigitalOutput = gpio.provisionDigitalOutputPin(trigPin)

  val echoPin: Pin = RaspiBcmPin.GPIO_21
  val echoInput: GpioPinDigitalInput =
    gpio.provisionDigitalInputPin(echoPin, PinPullResistance.PULL_DOWN)

  def apply(): Option[UltraSonicReading] = {

    logger.debug(s"measure distance with sensor echo $echoPin and trig $trigPin")

    try {
      triggerOuptut.high() // Make trigger pin HIGH

      Thread.sleep(0.01.toLong) // Delay for 10 microseconds

      triggerOuptut.low() //Make trigger pin LOW

      while ({
        triggerOuptut.isLow
      }) {
        //Wait until the ECHO pin gets HIGH
      }
      val startTime = System.nanoTime // Store the surrent time to calculate ECHO pin HIGH time.
      while ({
        echoInput.isHigh
      }) {
        //Wait until the ECHO pin gets LOW
      }
      val endTime = System.nanoTime // Store the echo pin HIGH end time to calculate ECHO pin HIGH time.
      val result: Double = (((endTime - startTime) / 1e3) / 2) / 29.1

      Some(UltraSonicReading(Some(result)))

    } catch {
      case e: InterruptedException =>
        logger.warn(s"$e", e)
        None
    }

  }

}
