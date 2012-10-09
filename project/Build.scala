/*
 * Copyright 2011 - 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import sbt.Keys._
import com.typesafe.sbtscalariform.ScalariformPlugin
import com.typesafe.sbtscalariform.ScalariformPlugin.ScalariformKeys

object Build extends sbt.Build {

  lazy val buildSettings = Seq(
    organization := "org.zeromq",
    name         := "zeromq-scala-binding",
    version      := "1.0.0-SNAPSHOT",
    scalaVersion := "2.10.0-M7",
    licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/valotrading/zeromq-scala-binding"))
  )

  lazy val zeromq_scala_binding = Project(
    id = "zeromq-scala-binding",
    base = file("."),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.zeromq
    )
  )

  lazy val defaultSettings = super.settings ++ Defaults.defaultSettings ++ buildSettings ++ publishToSettings ++ formatSettings ++ Seq(
    resolvers ++= Seq(
      "Typesafe Release Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshot Repository" at "http://repo.typesafe.com/typesafe/snapshots/"),
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-unchecked"),
    javacOptions in Compile ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation"),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    publishArtifact in Compile := true,
    publishArtifact in Test := false,
    exportJars := true,
    testOptions in Test += Tests.Argument("-oD"),
    // to fix scaladoc generation
    fullClasspath in doc in Compile <<= fullClasspath in Compile,
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
 )

  lazy val publishToSettings = Seq(
    pomIncludeRepository := { _ => false },
    publishMavenStyle := true,
    pomExtra := zeromqPomExtra,
    publishTo <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.endsWith("-SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else                             Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  )

  def zeromqPomExtra = {
    (<scm>
      <url>https://github.com/valotrading/zeromq-scala-binding</url>
    </scm>
      <developers>
        <developer>
          <id>kro</id>
          <name>Karim Vuorisara</name>
          <url>http://github.com/kro/</url>
        </developer>
        <developer>
          <id>helena</id>
          <name>Helena Edelson</name>
          <url>http://github.com/helena/</url>
        </developer>
      </developers>
    )
  }

  lazy val formatSettings = ScalariformPlugin.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  }
}

object Dependencies {
  import Dependency._

  val zeromq = Seq(jna, jnr, scalatest)
}

object Dependency {
  val jna         = "net.java.dev.jna"   % "jna"             % "3.4.0"                      // LGPL 2.1
  val jnr         = "com.github.jnr"     % "jnr-constants"   % "0.8.3"                      // Apache 2
  val scalatest   = "org.scalatest"      % "scalatest"       % "1.9-2.10.0-M7-B1" % "test" cross CrossVersion.full // Apache 2
}



