package onextent.iot.pijvmpoc2.streams

import java.util.concurrent.ArrayBlockingQueue

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import onextent.iot.pijvmpoc2.models._

class ButtonSource(buttonPin: Int, tempPin: Int)(implicit system: ActorSystem)
    extends GraphStage[SourceShape[(Int, Command)]] {

  val out: Outlet[(Int, Command)] = Outlet("CommandSource")

  override val shape: SourceShape[(Int, Command)] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {

    val bQueue = new ArrayBlockingQueue[ReadCommand](1)

    val monitor = new Runnable {
      override def run(): Unit = {
        Thread.sleep(1000 * 3)
        //todo: read from gpio
        bQueue.put(ReadCommand())
      }
    }

    new Thread(monitor).start()

    new GraphStageLogic(shape) {

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit =
            push(out, (tempPin, bQueue.take()))
        }
      )
    }
  }

}
