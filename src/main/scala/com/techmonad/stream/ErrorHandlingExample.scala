package com.techmonad.stream

import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.{Keep, Sink, Source}

object ErrorHandlingExample extends App {

  implicit val system = ActorSystem("GraphExample")

  import system.dispatcher

  val decider: Supervision.Decider = {
    case e: ArithmeticException =>
      e.printStackTrace()
      Supervision.Resume
    case _                      => Supervision.Stop
  }
  val source = Source(0 to 5).map(10/ _)
  val runnableGraph =
    source
      .toMat(Sink.fold(0)(_ + _))(Keep.right)


  val withCustomSupervision = runnableGraph.withAttributes(ActorAttributes.supervisionStrategy(decider))

  val result = withCustomSupervision.run()

  result.onComplete(a => println(a))
}
