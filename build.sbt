organization := "org.zeromq"

name := "zeromq-scala-binding"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.0"

scalaBinaryVersion <<= scalaVersion

libraryDependencies ++= Seq(
  "net.java.dev.jna" %  "jna"           % "3.0.9",
  "com.github.jnr"   %  "jnr-constants" % "0.8.2",
  "org.scalatest"    %  "scalatest_2.10"     % "2.0.M5b" % "test"
)

scalacOptions := Seq("-deprecation", "-unchecked")

publishMavenStyle := true

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

sources in (Compile, doc) ~= (_ filter (_ => false))

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>https://github.com/valotrading/zeromq-scala-binding</url>
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

homepage := Some(url("https://github.com/valotrading/zeromq-scala-binding"))
