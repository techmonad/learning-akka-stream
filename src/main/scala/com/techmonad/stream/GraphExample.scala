package com.techmonad.stream

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ClosedShape, SourceShape, UniformFanOutShape}
import akka.util.ByteString

object GraphExample extends App {

  implicit val system = ActorSystem("GraphExample")

  import system.dispatcher

  val graph1: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val in = Source(1 to 10)
        val sink = Sink.foreach(println)
        val bcast = builder.add(Broadcast[Int](2))
        val merge = builder.add(Merge[Int](2))
        val square = Flow[Int].map(a => a * a)
        val cube = Flow[Int].map(a => a * a * a)
        in ~> bcast ~> square ~> merge ~> sink
        bcast ~> cube ~> merge
        ClosedShape
      }
    )

  //graph1.run()

  val squareSink = FileIO.toPath(Paths.get("data/graph-squares.txt"))
  val cubeSink = FileIO.toPath(Paths.get("data/graph-cubes.txt"))
  val square = Flow[Int].map(a => a * a)
  val cube = Flow[Int].map(a => a * a * a)
  val toByteString = Flow[Int].map(a => ByteString(s"$a \n"))

  val graph2 = RunnableGraph.fromGraph(
    GraphDSL.createGraph(squareSink, cubeSink)((_, _)) { implicit builder =>
      (sSink, cSink) =>
        import GraphDSL.Implicits._
        val bcast: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
        val source = Source(1 to 100)
        source ~> bcast.in
        bcast ~> square ~> toByteString ~> sSink.in
        bcast ~> cube ~> toByteString ~> cSink.in
        ClosedShape
    }
  )

  //graph2.run()


  val sinks = List(Sink.foreach(println), Sink.foreach(println))

  val graph3 = RunnableGraph.fromGraph(
    GraphDSL.create(sinks) { implicit builder =>
      sinkList =>
        import GraphDSL.Implicits._

        val source = Source(1 to 100)
        val bcast = builder.add(Broadcast[Int](sinkList.length))
        source ~> bcast
        sinkList.foreach(sink => bcast ~> sink)
        ClosedShape
    }
  )
  //  graph3.run()


  val pairs =
    Source.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val zip = builder.add(Zip[Int, Int]())

        def ints = Source.fromIterator(() => Iterator.from(1))

        ints.filter(_ % 2 == 0) ~> zip.in0
        ints.filter(_ % 2 != 0) ~> zip.in1
        SourceShape(zip.out)
      }
    )

  // pairs.runWith(Sink.head).onComplete(a => println(a))

  Source
    .combine(Source(1 to 5), Source(6 to 10))(Merge(_))
    .runWith(Sink.fold(0)(_ + _))
    .onComplete(a => println(a))


}
