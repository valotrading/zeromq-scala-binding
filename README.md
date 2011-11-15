Scala Binding for ZeroMQ
========================

The Scala binding for ZeroMQ is based on ZeroMQ versions 2.1.x and uses JNA for accessing native functions. The Scala binding is a thin wrapper of the ZeroMQ API, however, the Scala binding offers a jzmq like API, which intends to be compatible with the original API so that users that want to access ZeroMQ over JNA can switch over to use the Scala binding.

Installation and usage
----------------------

Create a file, `project/plugins/build.sbt`, for plugin library dependencies with the following lines:

    resolvers += "Typesafe Repository (releases)" at "http://repo.typesafe.com/typesafe/releases/"

    libraryDependencies += "org.zeromq" %% "zeromq-scala-binding" % "0.0.3"
