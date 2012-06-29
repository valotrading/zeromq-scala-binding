# Scala Binding for ZeroMQ

This Scala binding for ZeroMQ is based on ZeroMQ versions 2.1.x and uses JNA for accessing native functions.
The Scala binding is a thin wrapper of the ZeroMQ API, however, the Scala binding offers a `jzmq`-like API, which is
intendedto be compatible with the original API; users of the original API who want to access ZeroMQ over JNA can switch
over to this Scala binding.

This version adds logging.
To enable in your program, added the following to `src/main/resource/logback-test.xml`, and set
`akka.event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]`:

````
<logger name="akka.zeromq" level="DEBUG"/>
<root level="DEBUG">
  <appender-ref ref="STDOUT"/>
</root>
````

## Installation and Usage

Mike Slinn modified these instructions so others could build and install locally, until such time as this version
might be accepted by upstream as a pull request.

 1. Follow the Developers instructions below.
 1. Create `project/plugins/build.sbt` for plug-in library dependencies with the following lines:

````
resolvers += "Typesafe Repository (releases)" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.zeromq" %% "zeromq-scala-binding" % "0.0.8-SNAPSHOT"
````

## Developers

 1. `git clone` the [javadoc-sbt plug-in](https://www.google.com/search?q=http%3A%2F%2Fjavadoc.javadocplugin.javadocsettings%2F&ie=utf-8&oe=utf-8&client=ubuntu&channel=fs),
and type:

````
cd javadoc-sbt
sbt publish-local
````

 1. Add this line to `project/plugins.sbt`:
````
addSbtPlugin("org.smop" % "javadoc-sbt" % "0.1.0-SNAPSHOT")
````
