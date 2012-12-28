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

import com.sun.jna.{Library, Native, NativeLong, Pointer}
import com.sun.jna.ptr.LongByReference
import jnr.constants.platform.Errno
import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.LongByReference
import java.util.Arrays
import java.lang.{ Long ⇒ JLong, Integer ⇒ JInteger }
import scala.beans.BeanProperty
import scala.annotation.tailrec

object ZeroMQ {

  /** Socket types */
  val ZMQ_PAIR   = 0
  val ZMQ_PUB    = 1
  val ZMQ_SUB    = 2
  val ZMQ_REQ    = 3
  val ZMQ_REP    = 4
  val ZMQ_DEALER = 5
  val ZMQ_ROUTER = 6
  val ZMQ_PULL   = 7
  val ZMQ_PUSH   = 8
  val ZMQ_XPUB   = 9
  val ZMQ_XSUB   = 10

  /** Send / receive options */
  val ZMQ_NOBLOCK = 1
  val ZMQ_SNDMORE = 2 

  /** Socket options */
  val ZMQ_HWM               = 1
  val ZMQ_SWAP              = 3
  val ZMQ_AFFINITY          = 4
  val ZMQ_IDENTITY          = 5
  val ZMQ_SUBSCRIBE         = 6
  val ZMQ_UNSUBSCRIBE       = 7
  val ZMQ_RATE              = 8
  val ZMQ_RECOVERY_IVL      = 9
  val ZMQ_MCAST_LOOP        = 10
  val ZMQ_SNDBUF            = 11
  val ZMQ_RCVBUF            = 12
  val ZMQ_RCVMORE           = 13
  val ZMQ_FD                = 14
  val ZMQ_EVENTS            = 15
  val ZMQ_TYPE              = 16
  val ZMQ_LINGER            = 17
  val ZMQ_RECONNECT_IVL     = 18
  val ZMQ_BACKLOG           = 19
  val ZMQ_RECONNECT_IVL_MAX = 21
  val ZMQ_MAXMSGSIZE        = 22
  val ZMQ_SNDHWM            = 23
  val ZMQ_RCVHWM            = 24
  val ZMQ_MULTICAST_HOPS    = 25
  val ZMQ_RCVTIMEO          = 27
  val ZMQ_SNDTIMEO          = 28

  /** Built-in devices */
  val ZMQ_STREAMER  = 1
  val ZMQ_FORWARDER = 2
  val ZMQ_QUEUE     = 3

  /** Unix errors */
  val EINVAL = Errno.EINVAL.intValue
  val EAGAIN = Errno.EAGAIN.intValue

  /** ZMQ errors */
  val ZMQ_HAUSNUMERO = 156384712
  val EFSM           = ZMQ_HAUSNUMERO + 51
  val ENOCOMPATPROTO = ZMQ_HAUSNUMERO + 52
  val ETERM          = ZMQ_HAUSNUMERO + 53

  /** ZMQ message definition */
  val ZMQ_MAX_VSM_SIZE = 30
  val ZMQ_DELIMITER    = 31
  val ZMQ_VSM          = 32
  val ZMQ_MSG_MORE     = 1
  val ZMQ_MSG_SHARED   = 128
  val ZMQ_MSG_MASK     = 129

  /** IO multiplexing */
  val ZMQ_POLLIN = 1: Short
  val ZMQ_POLLOUT = 2: Short
  val ZMQ_POLLERR = 4: Short

  def loadLibrary(): ZeroMQLibrary = Native.loadLibrary("zmq", classOf[ZeroMQLibrary]).asInstanceOf[ZeroMQLibrary]
}

trait ZeroMQLibrary extends Library {
  def zmq_bind(socket: Pointer, endpoint: String): Int
  def zmq_close(socket: Pointer): Int
  def zmq_connect(socket: Pointer, endpoint: String): Int
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
  def zmq_msg_size(msg: zmq_msg_t): Int
  def zmq_poll(items: Array[zmq_pollitem_t], nitems: Int, timeout: NativeLong): Int
  def zmq_recv(socket: Pointer, msg: zmq_msg_t, flags: Int): Int
  def zmq_send(socket: Pointer, msg: zmq_msg_t, flags: Int): Int
  def zmq_setsockopt(socket: Pointer, option_name: Int, option_value: Pointer, option_len: NativeLong): Int
  def zmq_socket(context: Pointer, socket_type: Int): Pointer
  def zmq_strerror(errnum: Int): String
  def zmq_term(context: Pointer): Int
  def zmq_version(major: Array[Int], minor: Array[Int], patch: Array[Int]): Unit
}

final case class ZMQException(val message: String, @BeanProperty val errorCode: Int) extends RuntimeException

/**
 * Offers an API similar to that of jzmq [1] written by Gonzalo Diethelm.
 * <p/>
 * 1. https://github.com/zeromq/jzmq
 */
object ZMQ {
  def makeVersion(major: Int, minor: Int, patch: Int): Int = major * 10000 + minor * 100 + patch
  
  def context(ioThreads: Int): Context = new Context(ioThreads)

  private final val zmq: ZeroMQLibrary = ZeroMQ.loadLibrary

  @BeanProperty final val (majorVersion: Int, minorVersion: Int, patchVersion: Int, fullVersion: Int, versionString: String) = {
    val ma, mi, pa = Array[Int](0)
    zmq.zmq_version(ma, mi, pa)
    (ma(0), mi(0), pa(0), makeVersion(ma(0), mi(0), pa(0)), "%d.%d.%d".format(ma(0), mi(0), pa(0)))
  }
  
  final val NOBLOCK = ZeroMQ.ZMQ_NOBLOCK
  final val DONTWAIT = ZeroMQ.ZMQ_NOBLOCK
  final val PAIR = ZeroMQ.ZMQ_PAIR
  final val SNDMORE = ZeroMQ.ZMQ_SNDMORE
  final val PUB = ZeroMQ.ZMQ_PUB
  final val SUB = ZeroMQ.ZMQ_SUB
  final val REQ = ZeroMQ.ZMQ_REQ
  final val REP = ZeroMQ.ZMQ_REP
  final val XREQ = ZeroMQ.ZMQ_DEALER
  final val XREP = ZeroMQ.ZMQ_ROUTER
  final val DEALER = ZeroMQ.ZMQ_DEALER
  final val ROUTER = ZeroMQ.ZMQ_ROUTER
  final val PULL = ZeroMQ.ZMQ_PULL
  final val PUSH = ZeroMQ.ZMQ_PUSH
  final val STREAMER = ZeroMQ.ZMQ_STREAMER
  final val FORWARDER = ZeroMQ.ZMQ_FORWARDER
  final val QUEUE = ZeroMQ.ZMQ_QUEUE

  class Context(ioThreads: Int) {
    protected[zeromq] final val ptr: Pointer = zmq.zmq_init(ioThreads)
    def term(): Unit = zmq.zmq_term(ptr)
    def socket(`type`: Int): Socket = new Socket(this, `type`)
    def poller(): Poller = poller(32)
    def poller(size: Int): Poller = new Poller(this, size)
  }

  private final val versionBelow210 = fullVersion < makeVersion(2, 1, 0)
  private final val versionAtleast210 = !versionBelow210
  private final val versionBelow220 = fullVersion < makeVersion(2, 2, 0)
  private final val versionAtleast220 = !versionBelow220
  private final val versionBelow300 = fullVersion < makeVersion(3, 0, 0)
  private final val versionAtleast300 = !versionBelow300

  class Socket(context: ZMQ.Context, `type`: Int) {
    import ZeroMQ._
    private final class MessageDataBuffer extends zmq_free_fn {
      private final val buffer = new java.util.HashSet[Pointer]
      def add(data: Pointer): Unit = buffer.add(data)
      override def invoke(data: Pointer, memory: Pointer): Unit = buffer.remove(memory)
    }

    private[zeromq] final val ptr: Pointer = zmq.zmq_socket(context.ptr, `type`)
    private final val messageDataBuffer = new MessageDataBuffer

    def close(): Unit = zmq.zmq_close(ptr)

    def getType(): Int = if (versionBelow210) -1 else getLongSockopt(ZMQ_TYPE).asInstanceOf[Int]

    def getLinger(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_LINGER)

    def getReconnectIVL(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_RECONNECT_IVL)

    def getBacklog(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_BACKLOG)

    def getReconnectIVLMax(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_RECONNECT_IVL_MAX)

    def getMaxMsgSize(): Long = if (versionBelow300) -1 else getLongSockopt(ZMQ_MAXMSGSIZE)

    def getSndHWM(): Int = if (versionBelow300) -1 else getIntSockopt(ZMQ_SNDHWM)

    def getRcvHWM(): Int = if (versionBelow300) -1 else getIntSockopt(ZMQ_RCVHWM)

    def getHWM(): Long = if (versionBelow300) getLongSockopt(ZMQ_HWM) else -1

    def getSwap(): Long = if (versionBelow300) -1 else getLongSockopt(ZMQ_SWAP)

    def getAffinity(): Long = getLongSockopt(ZMQ_AFFINITY)

    def getIdentity(): Array[Byte] = getBytesSockopt(ZMQ_IDENTITY)

    def getRate(): Long = getLongSockopt(ZMQ_RATE)

    def getRecoveryInterval(): Long = getLongSockopt(ZMQ_RECOVERY_IVL)

    def hasMulticastLoop(): Boolean = if (versionBelow300) false else getLongSockopt(ZMQ_MCAST_LOOP) != 0

    def setMulticastHops(mcast_hops: Long): Unit = setLongSockopt(ZMQ_MCAST_LOOP, mcast_hops)

    def getMulticastHops(): Long = if (versionBelow300) -1 else getLongSockopt(ZMQ_MCAST_LOOP)

    def setReceiveTimeOut(timeout: Int): Unit = if (versionAtleast220) setIntSockopt(ZMQ_RCVTIMEO, timeout)

    def getReceiveTimeOut(): Int = if (versionBelow220) -1 else getIntSockopt(ZMQ_RCVTIMEO)

    def setSendTimeOut(timeout: Int): Unit = if (versionAtleast220) setIntSockopt(ZMQ_SNDTIMEO, timeout)

    def getSendTimeOut(): Int = if (versionBelow220) -1 else getIntSockopt(ZMQ_SNDTIMEO)

    def getSendBufferSize(): Long = getLongSockopt(ZMQ_SNDBUF)

    def getReceiveBufferSize(): Long = getLongSockopt(ZMQ_RCVBUF)

    def hasReceiveMore(): Boolean = getLongSockopt(ZMQ_RCVMORE) != 0

    def getFD(): Long = if (versionBelow210) -1 else getLongSockopt(ZMQ_FD)

    def getEvents(): Long = if (versionBelow210) -1 else getLongSockopt(ZMQ_EVENTS)

    def setLinger(linger: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_LINGER, linger)

    def setReconnectIVL(reconnectIVL: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_RECONNECT_IVL, reconnectIVL)

    def setBacklog(backlog: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_BACKLOG, backlog)

    def setReconnectIVLMax(reconnectIVLMax: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_RECONNECT_IVL_MAX, reconnectIVLMax)

    def setMaxMsgSize(maxMsgSize: Long): Unit = if (versionAtleast300) setLongSockopt(ZMQ_MAXMSGSIZE, maxMsgSize)

    def setSndHWM(sndHWM: Int): Unit = if (versionAtleast300) setIntSockopt(ZMQ_SNDHWM, sndHWM)

    def setRcvHWM(rcvHWM: Int): Unit = if (versionBelow300) setIntSockopt(ZMQ_RCVHWM, rcvHWM)

    def setHWM(hwm: Long): Unit = if (versionBelow300) setLongSockopt(ZMQ_HWM, hwm)

    def setSwap(swap: Long): Unit = if (versionAtleast300) setLongSockopt(ZMQ_SWAP, swap)

    def setAffinity(affinity: Long): Unit = setLongSockopt(ZMQ_AFFINITY, affinity)

    def setIdentity(identity: Array[Byte]): Unit = setBytesSockopt(ZMQ_IDENTITY, identity)

    def subscribe(topic: Array[Byte]): Unit = setBytesSockopt(ZMQ_SUBSCRIBE, topic)

    def unsubscribe(topic: Array[Byte]): Unit = setBytesSockopt(ZMQ_UNSUBSCRIBE, topic)

    def setRate(rate: Long): Unit = setLongSockopt(ZMQ_RATE, rate)

    def setRecoveryInterval(recovery_ivl: Long): Unit = setLongSockopt(ZMQ_RECONNECT_IVL, recovery_ivl)

    def setMulticastLoop(mcast_loop: Boolean): Unit = if (versionBelow300) setLongSockopt(ZMQ_MCAST_LOOP, if (mcast_loop) 1 else 0)

    def setSendBufferSize(sndbuf: Long): Unit = setLongSockopt(ZMQ_SNDBUF, sndbuf)

    def setReceiveBufferSize(rcvbuf: Long): Unit = setLongSockopt(ZMQ_RCVBUF, rcvbuf)

    def bind(addr: String): Unit = zmq.zmq_bind(ptr, addr)

    def connect(addr: String): Unit = zmq.zmq_connect(ptr, addr)

    def send(msg: Array[Byte], flags: Int): Boolean = {
      val message: zmq_msg_t = newZmqMessage(msg)
      zmq.zmq_send(ptr, message, flags) match {
        case 0 ⇒ if (zmq.zmq_msg_close(message) != 0) raiseZMQException else true
        case EAGAIN ⇒ if (zmq.zmq_msg_close(message) != 0) raiseZMQException else false
        case other ⇒
          zmq.zmq_msg_close(message)
          raiseZMQException
      }
    }

    def recv(flags: Int): Array[Byte] = {
      val message: zmq_msg_t = newZmqMessage
      zmq.zmq_recv(ptr, message, flags) match {
        case 0 ⇒
          val dataByteArray: Array[Byte] = zmq.zmq_msg_data(message).getByteArray(0, zmq.zmq_msg_size(message))
          if (zmq.zmq_msg_close(message) != 0) raiseZMQException else dataByteArray
        case EAGAIN ⇒ if (zmq.zmq_msg_close(message) != 0) raiseZMQException else null
        case other ⇒
          zmq.zmq_msg_close(message)
          raiseZMQException
      }
    }

    override protected def finalize: Unit = close

    private def getLongSockopt(option: Int): Long = {
      val value: Memory = new Memory(JLong.SIZE / 8)
      val length: LongByReference = new LongByReference(JLong.SIZE / 8)
      zmq.zmq_getsockopt(ptr, option, value, length)
      value.getLong(0)
    }

    private def setLongSockopt(option: Int, optval: Long): Unit = {
      val value: Memory = new Memory(JLong.SIZE / 8)
      value.setLong(0, optval)
      zmq.zmq_setsockopt(ptr, option, value, new NativeLong(JLong.SIZE / 8))
    }

    private def getIntSockopt(option: Int): Int = {
      val value: Memory = new Memory(JInteger.SIZE / 8)
      zmq.zmq_getsockopt(ptr, option, value, new LongByReference(JInteger.SIZE / 8))
      value.getInt(0)
    }

    private def setIntSockopt(option: Int, optval: Int): Unit = {
      val value: Memory = new Memory(JInteger.SIZE / 8)
      value.setInt(0, optval)
      zmq.zmq_setsockopt(ptr, option, value, new NativeLong(JInteger.SIZE / 8))
    }

    private def getBytesSockopt(option: Int): Array[Byte] = {
      val value: Memory = new Memory(1024)
      val length: LongByReference = new LongByReference(1024)
      zmq.zmq_getsockopt(ptr, option, value, length)
      value.getByteArray(0, length.getValue.asInstanceOf[Int])
    }

    private def setBytesSockopt(option: Int, optval: Array[Byte]): Unit =
      zmq.zmq_setsockopt(ptr, option,
        if (optval.length == 0) Pointer.NULL
        else {
            val mem = new Memory(optval.length)
            mem.write(0, optval, 0, optval.length)
            mem
        },
        new NativeLong(optval.length))

    private def newZmqMessage(msg: Array[Byte]): zmq_msg_t = {
      val message: zmq_msg_t = new zmq_msg_t
      msg.length match {
        case 0 ⇒ if (zmq.zmq_msg_init_size(message, new NativeLong(0)) != 0) raiseZMQException
        case len ⇒
          val mem: Memory = new Memory(len)
          mem.write(0, msg, 0, len)
          if (zmq.zmq_msg_init_data(message, mem, new NativeLong(len), messageDataBuffer, mem) != 0) raiseZMQException
          else messageDataBuffer.add(mem)
      }
      message
    }

    private def newZmqMessage: zmq_msg_t = {
      val message: zmq_msg_t = new zmq_msg_t
      if (zmq.zmq_msg_init(message) != 0) raiseZMQException
      else message
    }

    private def raiseZMQException: Nothing = {
      val errno: Int = zmq.zmq_errno
      val reason: String = zmq.zmq_strerror(errno)
      throw new ZMQException(reason, errno)
    }
  }

  class Poller (context: ZMQ.Context, size: Int) {
    @BeanProperty var timeout: Long = -1
    private var nextEventIndex: Int = 0
    private var maxEventCount: Int = size
    private var curEventCount: Int = 0
    private var sockets: Array[ZMQ.Socket] = new Array(size)
    private var events: Array[Short] = new Array(size)
    private var revents: Array[Short] = new Array(size)
    private var freeSlots: List[Int] = Nil

    def register(socket: ZMQ.Socket): Int = register(socket, ZeroMQ.ZMQ_POLLIN | ZeroMQ.ZMQ_POLLOUT | ZeroMQ.ZMQ_POLLERR)

    def register(socket: ZMQ.Socket, numEvents: Int): Int = {
      require(numEvents <= Short.MaxValue, "numEvents must be less or equal to Short.MaxValue")
      require(numEvents >= Short.MinValue, "numEvents must be greater or equal to Short.MinValue")

      val pos: Int = freeSlots match {
        case Nil ⇒
          if (nextEventIndex >= maxEventCount) {
            val newMaxEventCount: Int = maxEventCount + 16
            sockets = Arrays.copyOf(sockets, newMaxEventCount)
            events = Arrays.copyOf(events, newMaxEventCount)
            revents = Arrays.copyOf(revents, newMaxEventCount)
            maxEventCount = newMaxEventCount
          }
          val p = nextEventIndex
          nextEventIndex += 1
          p
        case free :: tail ⇒
          freeSlots = tail
          free
      }
      sockets(pos) = socket
      events(pos) = numEvents.asInstanceOf[Short]
      curEventCount += 1
      pos
    }

    def unregister(socket: ZMQ.Socket): Unit = if (socket ne null) {
      @tailrec def unreg(index: Int): Unit =
        if (index >= nextEventIndex || index >= sockets.length) ()
        else if(sockets(index) eq socket) {
            sockets(index) = null
            events(index) = 0: Short
            revents(index) = 0: Short
            freeSlots ::= index
            curEventCount -= 1
        } else unreg(index + 1)
      
      unreg(0)
    }

    def getSocket(index: Int): ZMQ.Socket = if ((index < 0 || index >= nextEventIndex)) null else sockets(index)

    def getSize(): Int = maxEventCount

    def getNext(): Int = nextEventIndex

    def poll: Long = poll(this.timeout)

    def poll(timeout: Long): Long = {
      Arrays.fill(revents, 0, nextEventIndex, 0: Short)
      curEventCount match {
        case 0 ⇒ 0
        case expectedEvents ⇒
          // Goes through the items and either inits or collects revents depending on the "init" parameter,
          // returns the number of items procressed
          def withItems(items: Array[zmq_pollitem_t], init: Boolean): Int = {
            var itemIndex = 0
            var socketIndex = 0
            while (socketIndex < sockets.length) {
              if (sockets(socketIndex) ne null) {
                val item: zmq_pollitem_t = items(itemIndex)
                if (init) {
                  item.socket = sockets(socketIndex).ptr
                  item.fd = 0
                  item.events = events(socketIndex)
                  item.revents = 0: Short
                } else {
                  revents(socketIndex) = items(itemIndex).revents
                }
                itemIndex += 1
              }
              socketIndex += 1
            }
            itemIndex
          }
          val items = new zmq_pollitem_t().toArray(expectedEvents).asInstanceOf[Array[zmq_pollitem_t]]
          withItems(items, init = true) match {
            case `expectedEvents` ⇒
              val result: Int = zmq.zmq_poll(items, expectedEvents, new NativeLong(timeout))
              withItems(items, init = false)
              result
            case _ ⇒ 0 // Bail out
          }
      }
    }

    def pollin(index: Int): Boolean = poll_mask(index, ZeroMQ.ZMQ_POLLIN)

    def pollout(index: Int): Boolean = poll_mask(index, ZeroMQ.ZMQ_POLLOUT)

    def pollerr(index: Int): Boolean = poll_mask(index, ZeroMQ.ZMQ_POLLERR)

    private def poll_mask(index: Int, mask: Int): Boolean =
      if ((mask <= 0 || index < 0 || index >= nextEventIndex)) false else (revents(index) & mask) > 0
  }
}


