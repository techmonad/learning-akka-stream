package com.techmonad.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

object MergeStreams extends App {

  implicit val system = ActorSystem("AsyncStreamProcessing")


  Source(1 to 4)
    .map(_*2)
    .flatMapConcat(a => Source(1 to a).map(n => s"$n"*n) )
    .concat(Source(1 to 4))
    .runWith(Sink.foreach(println))

}
