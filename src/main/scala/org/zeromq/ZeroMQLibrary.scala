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

import com.sun.jna._
import com.sun.jna.ptr._

object ZeroMQ {
  /** Socket types */
  val ZMQ_PAIR = 0
  val ZMQ_PUB = 1
  val ZMQ_SUB = 2
  val ZMQ_REQ = 3
  val ZMQ_REP = 4
  val ZMQ_DEALER = 5
  val ZMQ_ROUTER = 6
  val ZMQ_PULL = 7
  val ZMQ_PUSH = 8
  val ZMQ_XPUB = 9
  val ZMQ_XSUB = 10
  /** Socket options */
  val ZMQ_HWM = 1
  val ZMQ_SWAP = 3
  val ZMQ_AFFINITY = 4
  val ZMQ_IDENTITY = 5
  val ZMQ_SUBSCRIBE = 6
  val ZMQ_UNSUBSCRIBE = 7
  val ZMQ_RATE = 8
  val ZMQ_RECOVERY_IVL = 9
  val ZMQ_MCAST_LOOP = 10
  val ZMQ_SNDBUF = 11
  val ZMQ_RCVBUF = 12
  val ZMQ_RCVMORE = 13
  val ZMQ_FD = 14
  val ZMQ_EVENTS = 15
  val ZMQ_TYPE = 16
  val ZMQ_LINGER = 17
  val ZMQ_RECONNECT_IVL = 18
  val ZMQ_BACKLOG = 19
  val ZMQ_RECONNECT_IVL_MAX = 21
  val ZMQ_MAXMSGSIZE = 22;
  val ZMQ_SNDHWM = 23;
  val ZMQ_RCVHWM = 24;
  val ZMQ_MULTICAST_HOPS = 25;
  val ZMQ_RCVTIMEO = 27;
  val ZMQ_SNDTIMEO = 28;
  /** Built-in devices */
  val ZMQ_STREAMER = 1
  val ZMQ_FORWARDER = 2
  val ZMQ_QUEUE = 3
  /** Unix errors */
  val EINVAL = 22
  val EAGAIN = 11
  /** ZMQ errors */
  val ZMQ_HAUSNUMERO = 156384712
  val EFSM = ZMQ_HAUSNUMERO + 51
  val ENOCOMPATPROTO = ZMQ_HAUSNUMERO + 52
  val ETERM = ZMQ_HAUSNUMERO + 53
  /** ZMQ message definition */
  val ZMQ_MAX_VSM_SIZE = 30
  val ZMQ_DELIMITER = 31
  val ZMQ_VSM = 32
  val ZMQ_MSG_MORE = 1
  val ZMQ_MSG_SHARED = 128
  val ZMQ_MSG_MASK = 129
  /** IO multiplexing */
  val ZMQ_POLLIN: Short = 1
  val ZMQ_POLLOUT: Short = 2
  val ZMQ_POLLERR: Short = 4 
  /** Helper for loading the ZeroMQ library */
  def loadLibrary: ZeroMQLibrary = {
    Native.loadLibrary("zmq", classOf[ZeroMQLibrary]).asInstanceOf[ZeroMQLibrary]  
  }
}

trait ZeroMQLibrary extends Library {
  def zmq_bind(socket: Pointer, endpoint: String): Int
  def zmq_close(socket: Pointer): Int
  def zmq_connect(scoket: Pointer, endpoint: String): Int
  def zmq_device(device: Int, frontend: Pointer, backend: Pointer): Int
  def zmq_errno: Int
  def zmq_getsockopt(socket: Pointer, option_name: Int, option_value: Pointer, option_len: LongByReference): Int
  def zmq_init(io_threads: Int): Pointer
  def zmq_msg_init(msg: zmq_msg_t): Int
  def zmq_msg_close(msg: zmq_msg_t): Int
  def zmq_msg_copy(dest: zmq_msg_t, src: zmq_msg_t): Int
  def zmq_msg_data(msg: zmq_msg_t): Pointer
  def zmq_msg_init_data(msg: zmq_msg_t, data: Pointer, size: NativeLong, ffn: zmq_free_fn, hint: Pointer): Int
  def zmq_msg_init_size(msg: zmq_msg_t, size: NativeLong): Int
  def zmq_msg_move(dest: zmq_msg_t, src: zmq_msg_t): Int
  def zmq_poll(items: Array[zmq_pollitem_t], nitems: Int, timeout: NativeLong): Int
  def zmq_recv(socket: Pointer, msg: zmq_msg_t, flags: Int): Int
  def zmq_send(socket: Pointer, msg: zmq_msg_t, flags: Int): Int
  def zmq_setsockopt(socket: Pointer, option_name: Int, option_value: Pointer, option_len: NativeLong): Int
  def zmq_socket(context: Pointer, socket_type: Int): Pointer
  def zmq_strerror(errnum: Int): String
  def zmq_term(context: Pointer): Int
  def zmq_version(major: Array[Int], minor: Array[Int], patch: Array[Int]): Unit
}
