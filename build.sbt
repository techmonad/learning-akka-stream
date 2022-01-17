name := "learning-akka-stream"

version := "0.1"

scalaVersion := "2.13.8"

val AkkaVersion = "2.6.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
)