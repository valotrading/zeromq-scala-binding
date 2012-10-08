# Scala Binding for ZeroMQ

The Scala binding for ZeroMQ is based on ZeroMQ versions 3.2.0 and uses [JNA][]
for accessing native functions. It is a thin wrapper of the ZeroMQ API, but
offers also a `jzmq`-like API intending to be compatible with the Java binding
for ZeroMQ; users of the Java binding wanting to access ZeroMQ over JNA can
switch over to the Scala binding.

[JNA]: https://github.com/twall/jna

## Installation

Scala binding for ZeroMQ is made available through a Maven repository. If
you are using SBT, amend your `build.sbt` or Build.scala with:

````
resolvers += "Sonatype (releases)" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "org.zeromq" %% "zeromq-scala-binding" % "1.0.0-SNAPSHOT"
````
