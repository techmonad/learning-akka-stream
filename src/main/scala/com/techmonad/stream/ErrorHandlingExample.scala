package com.techmonad.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, RestartSource, Sink, Source}
import akka.stream.{ActorAttributes, KillSwitches, RestartSettings, Supervision}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

object ErrorHandlingExample extends App {

  implicit val system = ActorSystem("GraphExample")

  import system.dispatcher


  /**
   * Supervision strategy
   */
  val decider: Supervision.Decider = {
    case NonFatal(th) =>
      th.printStackTrace()
      Supervision.Resume
  }

  val source =
    Source(-5 to 5)
      .map(1 / _) //throwing ArithmeticException: / by zero
  //1 using supervision strategy
  source
    .withAttributes(ActorAttributes.supervisionStrategy(decider)) // skip the element that causing the error
    .run()
    .onComplete(a => println(a))

  //2 using recover
  source
    .run()
    .recover { case NonFatal(th) =>
      th.printStackTrace()
      0
    }
    .onComplete(a => println(a))

  Source(0 to 10)
    .map { n =>
      if (n < 5) n.toString
      else {
        throw new RuntimeException("Boom!")
      }
    }
    .recoverWithRetries(attempts = 3, {
      case ex: RuntimeException =>
        ex.printStackTrace()
        Source(List("five", 1 / 0, "seven", "eight"))
    })
  //.runForeach(println)


  val settings = RestartSettings(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
  ).withMaxRestarts(20, 5.minutes) // limits the amount of restarts to 20 within 5 minutes

  val restartSource = RestartSource.withBackoff(settings) { () =>
    Source.future {
      // http call
      Future {
        println("Reading...................")
        val time = System.currentTimeMillis()
        if (System.currentTimeMillis() % 2 == 0) time else throw new IllegalArgumentException("ERROR: ")
      }
    }
  }

  val killSwitch = restartSource
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.foreach(event => println(s"Got event: $event")))(Keep.left)
    .run()


  Future {
    Thread.sleep(30000)
    println("Shutdown now.......")
    killSwitch.shutdown
  }


}
