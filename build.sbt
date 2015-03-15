
name := "event-store-client"

version := "0.0.1"

organization := ""

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "2.6",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)

EventStorePlugin.settings