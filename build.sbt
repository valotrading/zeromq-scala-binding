organization := "org.zeromq"

name := "zeromq-scala-binding"

version := "0.0.8-SNAPSHOT"

//scalaBinaryVersion := "2.10.0-M7"

libraryDependencies ++= Seq(
  "net.java.dev.jna" %  "jna"             % "3.0.9",
  "com.github.jnr"   %  "jnr-constants"   % "0.8.2",
  //"ch.qos.logback"   %  "logback-classic" % "1.0.0", // uncomment to see logging output
  "org.slf4j"        %  "slf4j-api"       % "1.6.4",
  "org.scalatest"    %% "scalatest"       % "1.6.1" % "test" //To build with Scala 2.9.2
  //"org.scalatest"    %% "scalatest"       % "1.9-2.10.0-M7-B1" % "test" //To build with Scala 2.9.2
)

scalacOptions := Seq("-deprecation", "-unchecked")

publishMavenStyle := true

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

seq(javadoc.JavadocPlugin.javadocSettings: _*)

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
