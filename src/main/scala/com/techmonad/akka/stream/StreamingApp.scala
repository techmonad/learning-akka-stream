package com.techmonad.akka.stream

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, IOResult}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}

import scala.concurrent.Future

object StreamingApp extends App {

  implicit val system = ActorSystem("StreamingApp")
  implicit val m = ActorMaterializer()
  val source: Source[Int, NotUsed] = Source(1 to 10)
  val runnableSource: Future[Done] = source.runForeach(println)

  import system.dispatcher


  source.map(_.toString).runWith(linkSink("output.txt"))
 runnableSource.onComplete(_ => system.terminate())

  def linkSink(fileName:String): Sink[String, Future[IOResult]] ={
    Flow[String]
      .map{str => ByteString(str + "\n") }
      .toMat(FileIO.toPath(Paths.get(fileName)))(Keep.right)
  }
}

object Stream1{

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = body.split(" ").collect {
      case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
    }.toSet
  }

  val akkaTag = Hashtag("#akka")
  val tweets: Source[Tweet, NotUsed] = ???
  val writeAuthors: Sink[Author, NotUsed] = ???
  val writeHashtags: Sink[Hashtag, NotUsed] = ???

  val g: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val bcast = b.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
    ClosedShape
  })
  g.run()
}

object CountStream{

  import Stream1._

  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ => 1)

  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  val counterGraph: RunnableGraph[Future[Int]] =
    tweets
      .via(count)
      .toMat(sumSink)(Keep.right)

  val sum: Future[Int] = counterGraph.run()

  sum.foreach(c => println(s"Total tweets processed: $c"))
}