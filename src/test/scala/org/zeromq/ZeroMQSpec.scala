package org.zeromq

import java.util.concurrent.{Executors, TimeUnit}
import org.scalatest.BeforeAndAfter
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.zeromq.ZeroMQ._
import scala.util.Random

import com.sun.jna._
import com.sun.jna.ptr._

class ZeroMQSpec extends WordSpec with MustMatchers with BeforeAndAfter {
  "ZeroMQ" must {
    var zmq: ZeroMQ = null
    var endpoint: String = null
    before {
      zmq = ZeroMQ.loadLibrary
      endpoint = "tcp://127.0.0.1:" + randomPort
    }
    "zmq_bind" in {
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_bind(socket, endpoint) must equal(0)
    }
    "zmq_close" in { 
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_close(socket) must equal(0)
    }
    "zmq_connect" in {
      val context = zmq.zmq_init(1)
      val (pub, sub) = (
        zmq.zmq_socket(context, ZMQ_PUB), 
        zmq.zmq_socket(context, ZMQ_SUB)
      )
      zmq.zmq_bind(pub, endpoint)
      zmq.zmq_connect(sub, endpoint) must equal(0)
    }
    "zmq_device" in {
      val context = zmq.zmq_init(1)
      val (frontend, backend) = (
        zmq.zmq_socket(context, ZMQ_DEALER), 
        zmq.zmq_socket(context, ZMQ_ROUTER)
      )
      zmq.zmq_bind(frontend, "tcp://127.0.0.1:" + randomPort)
      zmq.zmq_bind(backend, "tcp://127.0.0.1:" + randomPort)
      val executor = Executors.newSingleThreadScheduledExecutor
      executor.schedule(new Runnable { def run { zmq.zmq_term(context) } }, 1, TimeUnit.SECONDS)
      zmq.zmq_device(ZMQ_QUEUE, frontend, backend) must equal(-1)
      zmq.zmq_errno must equal(ZeroMQ.ETERM)
    }
    "zmq_errno" in { 
      zmq.zmq_init(-1)
      zmq.zmq_errno must equal (EINVAL)
    }
    "zmq_getsockopt / zmq_setsockopt" in {
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      val (offset, sizeInBytes, optionValue) = (0, 8, 1234)
      val value = new Memory(sizeInBytes) { setInt(offset, optionValue) }
      val (length, lengthRef) = (new NativeLong(sizeInBytes), new LongByReference(sizeInBytes))
      zmq.zmq_setsockopt(socket, ZMQ_RECOVERY_IVL, value, length) must equal(0)
      zmq.zmq_getsockopt(socket, ZMQ_RECOVERY_IVL, value, lengthRef) must equal(0)
      value.getInt(offset) must equal(optionValue)
    }
    "zmq_init" in { 
      val context = zmq.zmq_init(1)
      context must not be (null)
    }
    "zmq_msg_close" in {
      val msg = new zmq_msg_t
      zmq.zmq_msg_init(msg)
      zmq.zmq_msg_close(msg) must equal(0)
    }
    "zmq_msg_copy" in {
      val (dst, src) = (new zmq_msg_t, new zmq_msg_t)
      zmq.zmq_msg_init_data(src, dataMemory, new NativeLong(dataBytes.length), null, null)
      zmq.zmq_msg_init_size(dst, new NativeLong(dataBytes.length)) must equal(0)
      zmq.zmq_msg_copy(dst, src) must equal(0)
      zmq.zmq_msg_close(dst)
      zmq.zmq_msg_close(src)
    }
    "zmq_msg_data" in { 
      val msg = new zmq_msg_t
      zmq.zmq_msg_init(msg)
      zmq.zmq_msg_init_data(msg, dataMemory, new NativeLong(dataBytes.length), null, null)
      zmq.zmq_msg_data(msg).getByteArray(0, dataBytes.length) must equal(dataBytes)
      zmq.zmq_msg_close(msg)
    }
    "zmq_msg_init_data" in { 
      val msg = new zmq_msg_t
      zmq.zmq_msg_init_data(msg, dataMemory, new NativeLong(dataBytes.length), null, null) must equal(0)
      zmq.zmq_msg_close(msg)
    }
    "zmq_msg_init_size" in { 
      val msg = new zmq_msg_t
      zmq.zmq_msg_init_size(msg, new NativeLong(dataBytes.length)) must equal(0)
      zmq.zmq_msg_close(msg)
    }
    "zmq_msg_init" in {
      val msg = new zmq_msg_t
      zmq.zmq_msg_init(msg) must equal(0)
      zmq.zmq_msg_close(msg)
    }
    "zmq_msg_move" in { 
      val (dst, src) = (new zmq_msg_t, new zmq_msg_t)
      zmq.zmq_msg_init_data(src, dataMemory, new NativeLong(dataBytes.length), null, null)
      zmq.zmq_msg_move(dst, src) must equal(0)
      zmq.zmq_msg_close(dst)
      zmq.zmq_msg_close(src)
    }
    "zmq_poll" ignore { }
    "zmq_recv" ignore { }
    "zmq_send" ignore { }
    "zmq_socket" in { 
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      socket must not be (null)
    }
    "zmq_strerror" in { 
      zmq.zmq_init(-1)
      zmq.zmq_strerror(ETERM) must equal("Context was terminated")
    }
    "zmq_term" in { 
      val context = zmq.zmq_init(1)
      zmq.zmq_term(context) must equal (0)
    }
    "zmq_version" in {
      val (major_x, minor_x, patch_x) = (Array(1), Array(1), Array(1))
      val (major_y, minor_y, patch_y) = (Array(1), Array(1), Array(1))
      zmq.zmq_version(major_x, minor_x, patch_x)
      zmq.zmq_version(major_y, minor_y, patch_y)
      (major_x(0), minor_x(0), patch_x(0)) must equal (major_y(0), minor_y(0), patch_y(0))
    }
  }
  def randomPort = 1024 + new Random(System.currentTimeMillis).nextInt(4096)
  lazy val dataBytes = "hello world".getBytes
  lazy val dataMemory = new Memory(dataBytes.length) { write(0, dataBytes, 0, dataBytes.length) }
}
