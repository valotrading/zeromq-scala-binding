organization := "org.zeromq"

name := "zeromq-scala-binding"

version := "0.0.7-SNAPSHOT"

libraryDependencies ++= Seq(
  "net.java.dev.jna" % "jna" % "3.0.9",
  "com.github.jnr" % "jnr-constants" % "0.8.2",
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in (Compile, packageDoc) := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>https://github.com/kro/zeromq-scala-binding</url>
    <connection>https://kro@github.com/kro/zeromq-scala-binding.git</connection>
  </scm>
  <developers>
    <developer>
      <id>kro</id>
      <name>Karim Vuorisara</name>
      <url>http://github.com/kro/</url>
    </developer>
  </developers>
)

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://jsuereth.com/scala-arm"))
