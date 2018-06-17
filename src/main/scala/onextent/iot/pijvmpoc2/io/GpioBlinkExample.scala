package onextent.iot.pijvmpoc2.io

import com.pi4j.io.gpio._

object GpioBlinkExample {

  def apply(): Unit = {

    println("blink demo starting")

    GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING))

    val gpio: GpioController = GpioFactory.getInstance()

    //val pin: Pin = RaspiPin.GPIO_16
    val pin: Pin = RaspiBcmPin.GPIO_16

    val led = gpio.provisionDigitalOutputPin(pin)

    led.blink(1000)

    Thread.sleep(1000 * 10)

    led.setState(false)
    gpio.shutdown()
    println("blink demo complete")

  }
}
