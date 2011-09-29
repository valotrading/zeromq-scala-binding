package org.zeromq

import com.sun.jna.{Library, Native}

object ZeroMQ {
  def loadLibrary: ZeroMQ = {
    Native.loadLibrary("zmq", classOf[ZeroMQ]).asInstanceOf[ZeroMQ]  
  }
}

trait ZeroMQ extends Library {
  def zmq_version(major: Array[Int], minor: Array[Int], patch: Array[Int]): Unit
}
