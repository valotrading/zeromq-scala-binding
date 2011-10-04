/*
 * Copyright 2011 the original author or authors.
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
package org.zeromq

import java.util.concurrent.{Executors, TimeUnit}
import org.scalatest.BeforeAndAfter
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.zeromq.ZeroMQ._
import scala.util.Random

import com.sun.jna._
import com.sun.jna.ptr._

class ZeroMQLibrarySpec extends WordSpec with MustMatchers with BeforeAndAfter {
  "ZeroMQLibrary" must {
    var zmq: ZeroMQLibrary = null
    var endpoint: String = null
    before {
      zmq = ZeroMQ.loadLibrary
      endpoint = "inproc://zeromq-spec"
    }
    "zmq_bind" in {
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_bind(socket, endpoint) must equal(0)
      zmq.zmq_close(socket)
    }
    "zmq_close" in { 
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_close(socket) must equal(0)
    }
    "zmq_connect" in {
      val context = zmq.zmq_init(1)
      val (pub, sub) = (zmq.zmq_socket(context, ZMQ_PUB), zmq.zmq_socket(context, ZMQ_SUB))
      zmq.zmq_bind(pub, endpoint)
      zmq.zmq_connect(sub, endpoint) must equal(0)
      zmq.zmq_close(sub)
      zmq.zmq_close(pub)
    }
    "zmq_device" in {
      val context = zmq.zmq_init(1)
      val (frontend, backend) = (zmq.zmq_socket(context, ZMQ_DEALER), zmq.zmq_socket(context, ZMQ_ROUTER))
      zmq.zmq_bind(frontend, "tcp://127.0.0.1:" + randomPort)
      zmq.zmq_bind(backend, "tcp://127.0.0.1:" + randomPort)
      val executor = Executors.newSingleThreadScheduledExecutor
      executor.schedule(new Runnable { def run { zmq.zmq_term(context) } }, 1, TimeUnit.SECONDS)
      zmq.zmq_device(ZMQ_QUEUE, frontend, backend) must equal(-1)
      zmq.zmq_errno must equal(ZeroMQ.ETERM)
      zmq.zmq_close(frontend)
      zmq.zmq_close(backend)
    }
    "zmq_errno" in { 
      zmq.zmq_init(-1)
      zmq.zmq_errno must equal (EINVAL)
    }
    "zmq_(get|set)sockopt" in {
      val context = zmq.zmq_init(1)
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      val (offset, sizeInBytes, optionValue) = (0, 8, 1234)
      val value = new Memory(sizeInBytes) { setInt(offset, optionValue) }
      val (length, lengthRef) = (new NativeLong(sizeInBytes), new LongByReference(sizeInBytes))
      zmq.zmq_setsockopt(socket, ZMQ_HWM, value, length) must equal(0)
      zmq.zmq_getsockopt(socket, ZMQ_HWM, value, lengthRef) must equal(0)
      value.getInt(offset) must equal(optionValue)
      zmq.zmq_close(socket)
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
      zmq.zmq_msg_init(dst)
      zmq.zmq_msg_move(dst, src) must equal(0)
      zmq.zmq_msg_close(dst)
      zmq.zmq_msg_close(src)
    }
    "zmq_msg_size" in {
      val msg = new zmq_msg_t
      zmq.zmq_msg_init_size(msg, new NativeLong(dataBytes.length))
      zmq.zmq_msg_size(msg) must equal(dataBytes.length)
      zmq.zmq_msg_close(msg)
    }
    "zmq_(poll|send|recv)" in { 
      val context = zmq.zmq_init(1)
      val (pub, sub) = (zmq.zmq_socket(context, ZMQ_PUB), zmq.zmq_socket(context, ZMQ_SUB))
      zmq.zmq_bind(pub, endpoint)
      zmq.zmq_connect(sub, endpoint)
      zmq.zmq_setsockopt(sub, ZMQ_SUBSCRIBE, Pointer.NULL, new NativeLong(0))
      val (outgoingMsg, incomingMsg) = (new zmq_msg_t, new zmq_msg_t)
      zmq.zmq_msg_init_data(outgoingMsg, dataMemory, new NativeLong(dataBytes.length), null, null)
      zmq.zmq_msg_init(incomingMsg)
      zmq.zmq_send(pub, outgoingMsg, 0) must equal(0)
      val items = new zmq_pollitem_t().toArray(1).asInstanceOf[Array[zmq_pollitem_t]]
      items(0) = new zmq_pollitem_t
      items(0).socket = sub
      items(0).events = ZMQ_POLLIN
      zmq.zmq_poll(items, 1, new NativeLong(-1)) must equal (1)
      zmq.zmq_recv(sub, incomingMsg, 0) must equal(0)
      zmq.zmq_msg_close(outgoingMsg)
      zmq.zmq_close(sub)
      zmq.zmq_close(pub)
    }
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
