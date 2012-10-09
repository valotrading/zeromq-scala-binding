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
package org.zeromq

import java.util.concurrent.{ CountDownLatch, Executors, TimeUnit }
import com.sun.jna.ptr.LongByReference
import com.sun.jna.{ Memory, NativeLong, Pointer }
import org.zeromq.ZeroMQ._
import org.zeromq.ZMQ._

class ZeroMQLibrarySpec extends AbstractZeroMQSpec {

  lazy val dataMemory = new Memory(dataBytes.length) {
    write(0, dataBytes, 0, dataBytes.length)
  }

  def createContext: Pointer = if (getFullVersion < makeVersion(3, 2, 0)) zmq.zmq_init(1) else zmq.zmq_ctx_new

  def destroyContext(p: Pointer): Int = if (getFullVersion < makeVersion(3, 2, 0)) zmq.zmq_term(p) else zmq.zmq_ctx_destroy(p)

  "ZeroMQLibrary" must {

    "zmq_ctx_new or zmq_init" in {
      val context = createContext
      context must not be (null)
      destroyContext(context) must equal(0)
    }
    "zmq_ctx_destroy or zmq_term" in {
      destroyContext(createContext) must equal(0)
    }
    "zmq_bind" in {
      val context = createContext
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_bind(socket, endpoint) must equal(0)
      zmq.zmq_close(socket) must equal(0)
      destroyContext(context) must equal(0)
    }
    "zmq_close" in {
      val context = createContext
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_close(socket) must equal(0)
      destroyContext(context) must equal(0)
    }
    "zmq_connect" in {
      val context = createContext
      val (pub, sub) = (zmq.zmq_socket(context, ZMQ_PUB), zmq.zmq_socket(context, ZMQ_SUB))
      zmq.zmq_bind(pub, endpoint) must equal(0)
      zmq.zmq_connect(sub, endpoint) must equal(0)
      zmq.zmq_close(sub) must equal(0)
      zmq.zmq_close(pub) must equal(0)
      destroyContext(context) must equal(0)
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
    "zmq_socket" in {
      val context = createContext
      val socket = zmq.zmq_socket(context, ZMQ_PUB)
      socket must not be (null)
    }
    "zmq_version" in {
      val (major_x, minor_x, patch_x) = (Array(1), Array(1), Array(1))
      val (major_y, minor_y, patch_y) = (Array(1), Array(1), Array(1))
      zmq.zmq_version(major_x, minor_x, patch_x)
      zmq.zmq_version(major_y, minor_y, patch_y)
      (major_x(0), minor_x(0), patch_x(0)) must equal(major_y(0), minor_y(0), patch_y(0))
    }
    "zmq_term" in {
      val context = createContext
      zmq.zmq_term(context) must equal(0)
    }
    "zmq_device" in {
      val context = createContext
      val (frontend, backend) = (zmq.zmq_socket(context, ZMQ_DEALER), zmq.zmq_socket(context, ZMQ_ROUTER))
      zmq.zmq_bind(frontend, bindTo)
      zmq.zmq_bind(backend, bindTo)
      val executor = Executors.newSingleThreadScheduledExecutor
      executor.schedule(new Runnable {
        def run {
          destroyContext(context)
        }
      }, 1, TimeUnit.SECONDS)
      zmq.zmq_device(ZMQ_QUEUE, frontend, backend) must equal(-1)
      zmq.zmq_errno must equal(ZeroMQ.ETERM)
      zmq.zmq_close(frontend)
      zmq.zmq_close(backend)
      executor.shutdown
    }
    "zmq_(get|set)sockopt" in {
      val context = createContext
      val pubSocket = zmq.zmq_socket(context, ZMQ_PUB)
      val subSocket = zmq.zmq_socket(context, ZMQ_SUB)
      val (offset, sizeInBytes, optionValue) = (0, if (getFullVersion < makeVersion(3, 2, 0)) 8 else ZMQ.intToByteArray(ZMQ_SNDHWM).length, 1234)
      val value = new Memory(sizeInBytes) { setInt(offset, optionValue) }
      val (length, lengthRef) = (new NativeLong(sizeInBytes), new LongByReference(sizeInBytes))

      if (getFullVersion < makeVersion(3, 2, 0)) {
        zmq.zmq_setsockopt(pubSocket, ZMQ_HWM, value, length) must equal(0)
        zmq.zmq_getsockopt(pubSocket, ZMQ_HWM, value, lengthRef) must equal(0)

        zmq.zmq_setsockopt(subSocket, ZMQ_HWM, value, length) must equal(0)
        zmq.zmq_getsockopt(subSocket, ZMQ_HWM, value, lengthRef) must equal(0)
      } else {
        zmq.zmq_setsockopt(pubSocket, ZMQ_SNDHWM, value, length) must equal(0)
        zmq.zmq_getsockopt(pubSocket, ZMQ_SNDHWM, value, lengthRef) must equal(0)

        zmq.zmq_setsockopt(subSocket, ZMQ_RCVHWM, value, length) must equal(0)
        zmq.zmq_getsockopt(subSocket, ZMQ_RCVHWM, value, lengthRef) must equal(0)
      }
      value.getInt(offset) must equal(optionValue)
      zmq.zmq_close(pubSocket)
      zmq.zmq_close(subSocket)
      destroyContext(context)
    }
    "zmq_(poll|send|recv)" in {
      val context = createContext
      val (pub, sub) = (zmq.zmq_socket(context, ZMQ_PUB), zmq.zmq_socket(context, ZMQ_SUB))
      zmq.zmq_bind(pub, endpoint)
      zmq.zmq_connect(sub, endpoint)
      zmq.zmq_setsockopt(sub, ZMQ_SUBSCRIBE, Pointer.NULL, new NativeLong(0))
      val (outgoingMsg, incomingMsg) = (new zmq_msg_t, new zmq_msg_t)
      zmq.zmq_msg_init_data(outgoingMsg, dataMemory, new NativeLong(dataBytes.length), null, null)
      zmq.zmq_msg_init(incomingMsg)
      if (getFullVersion < makeVersion(3, 2, 0)) zmq.zmq_recv(sub, incomingMsg, ZMQ_NOBLOCK) must be(-1)
      else zmq.zmq_msg_recv(incomingMsg, sub, ZMQ_DONTWAIT) must be(-1)
      zmq.zmq_errno must equal(EAGAIN)
      if (getFullVersion < makeVersion(3, 2, 0)) zmq.zmq_send(pub, outgoingMsg, 0) must equal(0)
      else zmq.zmq_msg_send(outgoingMsg, pub, 0) must equal(dataBytes.length)
      val items = new zmq_pollitem_t().toArray(1).asInstanceOf[Array[zmq_pollitem_t]]
      items(0) = new zmq_pollitem_t
      items(0).socket = sub
      items(0).events = ZMQ_POLLIN
      zmq.zmq_poll(items, 1, new NativeLong(-1)) must equal(1)
      if (getFullVersion < makeVersion(3, 2, 0)) zmq.zmq_recv(sub, incomingMsg, 0) must equal(0)
      else zmq.zmq_msg_recv(incomingMsg, sub, 0) must equal(dataBytes.length)
      zmq.zmq_msg_close(outgoingMsg)
      zmq.zmq_close(sub)
      zmq.zmq_close(pub)
      destroyContext(context)
    }
    "zmq_strerror" in {
      zmq.zmq_init(-1)
      zmq.zmq_strerror(ETERM) must equal("Context was terminated")
    }
    "zmq_errno" in {
      zmq.zmq_init(-1)
      zmq.zmq_errno must equal(EINVAL)
    }
    "fire and forget 100 messages within 1 seconds" in {
      val context = createContext
      val pub = zmq.zmq_socket(context, ZMQ_PUB)
      zmq.zmq_bind(pub, endpoint)
      val scale = 100
      val latch = new CountDownLatch(scale)
      for (i ← 0 to scale) {
        val msg = ("hello-" + i).getBytes
        val mem = new Memory(msg.length) {
          write(0, msg, 0, msg.length)
        }
        val outgoingMsg = new zmq_msg_t
        zmq.zmq_msg_init_data(outgoingMsg, mem, new NativeLong(msg.length), null, null)
        if (getFullVersion < makeVersion(3, 2, 0)) zmq.zmq_send(pub, outgoingMsg, 0) must equal(0)
        else zmq.zmq_msg_send(outgoingMsg, pub, 0) must equal(msg.length)
        mem.clear()
        zmq.zmq_msg_close(outgoingMsg)
      }
      latch.await(1, TimeUnit.SECONDS)
      zmq.zmq_close(pub)
      destroyContext(context)
    }
  }
}
