package com.techmonad.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

object AsyncStreamProcessing extends App {

  implicit val system = ActorSystem("AsyncStreamProcessing")

  import system.dispatcher

  def process = {
    val startTime = System.currentTimeMillis()
    Source(1 to 1000)
      .map(compute)
      .map(compute)
      .runWith(Sink.ignore)
      .onComplete { _ =>
        println("Total took to complete by process: " + (System.currentTimeMillis() - startTime))
      }
  }

  private def compute(v: Int) = {
    val time = System.currentTimeMillis()
    while (System.currentTimeMillis() - time < 10) {}
    v
  }

  def processUsingAsync = {
    val startTime = System.currentTimeMillis()
    Source(1 to 1000)
      .map(compute)
      .async
      .map(compute)
      .async
      .runWith(Sink.ignore)
      .onComplete { _ =>
        println("Total took to complete by processUsingAsync: " + (System.currentTimeMillis() - startTime))
      }
  }

  def processUsingMapAsync = {
    val startTime = System.currentTimeMillis()
    Source(1 to 1000)
      .mapAsync(8)(v => Future(compute(v)))
      .mapAsync(8)(v => Future(compute(v)))
      .runWith(Sink.ignore)
      .onComplete { _ =>
        println("Total took to complete by processUsingMapAsync: " + (System.currentTimeMillis() - startTime))
      }
  }


  def processUsingMapAsyncAndAsync = {
    val startTime = System.currentTimeMillis()
    Source(1 to 1000)
      .mapAsync(8)(v => Future(compute(v)))
      .async
      .mapAsync(8)(v => Future(compute(v)))
      .async
      .runWith(Sink.ignore)
      .onComplete { _ =>
        println("Total took to complete by processUsingMapAsyncAndAsync: " + (System.currentTimeMillis() - startTime))
      }
  }

  //process
  //processUsingAsync
  //processUsingMapAsync
  processUsingMapAsyncAndAsync

}


/*
Total took to complete by process: 20083ms
Total took to complete by processUsingAsync: 10115ms
Total took to complete by processUsingMapAsync 2897ms
Total took to complete by processUsingMapAsyncAndAsync: 2721
 */