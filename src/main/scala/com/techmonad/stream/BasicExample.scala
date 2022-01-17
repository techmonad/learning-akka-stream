package com.techmonad.stream

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration._

object BasicExample extends App {

  implicit val system = ActorSystem("BasicExample")

  //create source
  val source: Source[Int, NotUsed] = Source(1 to 100)

  // define the transformations
  val square: Flow[Int, ByteString, NotUsed] =
    Flow[Int]
      .map { i => ByteString(s"$i, ${i * i}\n") }

  val cube: Flow[Int, ByteString, NotUsed] =
    Flow[Int]
      .map { i => ByteString(s"$i, ${i * i * i}\n") }

  // create sink
  def sink(fileName: String): Sink[ByteString, Future[IOResult]] =
    FileIO.toPath(Paths.get(fileName))


  //  Wiring up a Source, Sink and Flow
  val runnable: RunnableGraph[Future[IOResult]] =
    source
      .via(square)
      .toMat(sink("data/squares.txt"))(Keep.right)

  runnable.run()

  //shorthand of above
  source
    .via(square)
    .runWith(sink("data/squares2.txt"))

  // Using throttling 10 elements in 1 seconds
  source
    .via(cube)
    .throttle(10, 1.seconds)
    .runWith(sink("data/cubes.txt"))

}
