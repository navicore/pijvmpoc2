package onextent.iot.pijvmpoc2.streams

import java.util.concurrent.ArrayBlockingQueue

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.pijvmpoc2.models._

class ButtonSource(buttonPin: Pin, tempPin: Int)(implicit system: ActorSystem)
    extends GraphStage[SourceShape[(Int, Command)]]
    with LazyLogging {

  val out: Outlet[(Int, Command)] = Outlet("CommandSource")

  override val shape: SourceShape[(Int, Command)] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {

    val bQueue = new ArrayBlockingQueue[ReadCommand](1)
    val gpio: GpioController = GpioFactory.getInstance

    val myButton: GpioPinDigitalInput =
      gpio.provisionDigitalInputPin(buttonPin, PinPullResistance.PULL_DOWN)

    myButton.addListener(new GpioPinListenerDigital() {
      override def handleGpioPinDigitalStateChangeEvent(
          event: GpioPinDigitalStateChangeEvent): Unit = {
        if (event.getState == PinState.LOW)
          try {
            logger.debug(s"button $buttonPin pressed")
            bQueue.offer(ReadCommand(), 100, java.util.concurrent.TimeUnit.DAYS)
          } catch {
            case e: Throwable => logger.warn(s"offer: $e")
          }

      }
    })

    new GraphStageLogic(shape) {

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            try {
              push(
                out,
                (tempPin, bQueue.poll(100, java.util.concurrent.TimeUnit.DAYS)))
            } catch {
              case e: Throwable => logger.warn(s"poll: $e")
            }
          }
        }
      )
    }

  }

}
