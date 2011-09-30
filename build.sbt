sbtPlugin := true

organization := "org.zeromq"

name := "zeromq-scala-bindings"

version := "0.0.1-SNAPSHOT"

libraryDependencies += "net.java.dev.jna" % "jna" % "3.0.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo := Some(Resolver.file("Github Pages", file("../zeromq-scala-bindings-gh-pages/maven/")))
