organization := "org.zeromq"

name := "zeromq-scala-binding"

version := "0.0.6"

libraryDependencies ++= Seq(
  "net.java.dev.jna" % "jna" % "3.0.9",
  "com.github.jnr" % "jnr-constants" % "0.8.2",
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo := Some(Resolver.file("GitHub Pages", file("../zeromq-scala-binding-gh-pages/maven/")))

publishArtifact in (Compile, packageDoc) := false 
