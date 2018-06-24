package onextent.iot.pijvmpoc2.io

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.models.UltraSonicReading

object SR04Sensor extends LazyLogging {

  val gpio: GpioController = GpioFactory.getInstance()

  val trigPin: Pin = RaspiBcmPin.GPIO_20
  val sensorTriggerPin: GpioPinDigitalOutput = gpio.provisionDigitalOutputPin(trigPin)

  val echoPin: Pin = RaspiBcmPin.GPIO_21
  val sensorEchoPin: GpioPinDigitalInput =
    gpio.provisionDigitalInputPin(echoPin, PinPullResistance.PULL_DOWN)

  def apply(): Option[UltraSonicReading] = {

    logger.debug(s"measure distance with sensor echo $echoPin and trig $trigPin")

    try {
      sensorTriggerPin.high() // Make trigger pin HIGH

      Thread.sleep(0.01.toLong) // Delay for 10 microseconds

      sensorTriggerPin.low() //Make trigger pin LOW

      logger.debug("ejs 1") // ejs this is the last log msg!!!!!!!!!!!!!!!!!!!!!!!
      while ({
        sensorEchoPin.isLow
      }) {
        //Wait until the ECHO pin gets HIGH
      }
      val startTime = System.nanoTime // Store the surrent time to calculate ECHO pin HIGH time.
      logger.debug("ejs 2")
      while ({
        sensorEchoPin.isHigh
      }) {
        //Wait until the ECHO pin gets LOW
      }
      logger.debug("ejs 3")
      val endTime = System.nanoTime // Store the echo pin HIGH end time to calculate ECHO pin HIGH time.
      val result: Double = (((endTime - startTime) / 1e3) / 2) / 29.1

      logger.debug(s"... distance by sensor echo $echoPin and trig $trigPin was $result")
      Some(UltraSonicReading(Some(result)))

    } catch {
      //case e: InterruptedException =>
      case e: Throwable =>
        logger.warn(s"$e", e)
        None
    }

  }

}
