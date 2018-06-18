package onextent.iot.pijvmpoc2.io

import com.pi4j.io.gpio._

object PwmLedExample {

  def apply(): Unit = {

    GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING))

    val gpio: GpioController = GpioFactory.getInstance()

    val pin: Pin = RaspiBcmPin.GPIO_18

    val pwm = gpio.provisionPwmOutputPin(pin)
    
    pwm.setPwmRange(100)

    pwm.setPwm(0)

    for (i <- 1 to 100) {
      pwm.setPwm(i)
      Thread.sleep(10)
    }

    for (i <- 100 to 1 by -1) {
      pwm.setPwm(i)
      Thread.sleep(10)
    }

    pwm.setPwm(0)
    gpio.shutdown()

  }
}
