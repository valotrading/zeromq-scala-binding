package org.zeromq

import org.scalatest.BeforeAndAfter
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class ZeroMQSpec extends WordSpec with MustMatchers with BeforeAndAfter {
  "ZeroMQ" must {
    var zmq: ZeroMQ = null
    before {
      zmq = ZeroMQ.loadLibrary
    }
    "implement zmq_version" in {
      val (major, minor, patch) = (Array(1), Array(1), Array(1))
      zmq.zmq_version(major, minor, patch)
      (major(0), minor(0)) must equal((2, 1))
    }
  }
}
