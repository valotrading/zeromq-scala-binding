organization := "org.zeromq"

name := "zeromq-scala-binding"

version := "0.0.4-SNAPSHOT"

libraryDependencies += "net.java.dev.jna" % "jna" % "3.0.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo := Some(Resolver.file("GitHub Pages", file("../zeromq-scala-binding-gh-pages/maven/")))

publishArtifact in (Compile, packageDoc) := false 
