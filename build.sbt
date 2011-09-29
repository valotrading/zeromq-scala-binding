sbtPlugin := true

organization := "org.zeromq"

name := "zeromq-scala-bindings"

version := "0.0.1-SNAPSHOT"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo := Some(Resolver.file("Github Pages", file("../zeromq-scala-bindings-gh-pages/maven/")))
