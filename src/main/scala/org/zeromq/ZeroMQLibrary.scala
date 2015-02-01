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
import java.util.{ Arrays, HashSet => JHashSet }
import java.lang.{ Long ⇒ JLong, Integer ⇒ JInteger }
import scala.beans.BeanProperty
import scala.annotation.tailrec
import concurrent.duration._


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

  /** Context options */
  val ZMQ_IO_THREADS =  1
  val ZMQ_MAX_SOCKETS = 2

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
  def zmq_msg_recv(msg: zmq_msg_t, socket: Pointer, flags: Int): Int
  def zmq_msg_send(msg: zmq_msg_t, socket: Pointer, flags: Int): Int
  def zmq_setsockopt(socket: Pointer, option_name: Int, option_value: Pointer, option_len: NativeLong): Int
  def zmq_socket(context: Pointer, socket_type: Int): Pointer
  def zmq_strerror(errnum: Int): String
  def zmq_term(context: Pointer): Int
  def zmq_ctx_set(context: Pointer, option_name: Int, option_value: Int): Boolean
  def zmq_version(major: Array[Int], minor: Array[Int], patch: Array[Int]): Unit
}

/**
 * ZMQException is used throughout the API to indicate failure
 * @param message the message to be used
 * @param errorCode the 0MQ Error Code
 */
final case class ZMQException(message: String, @BeanProperty val errorCode: Int) extends RuntimeException(message)

/**
 * Offers an API similar to that of jzmq [1] written by Gonzalo Diethelm.
 * <p/>
 * 1. https://github.com/zeromq/jzmq
 */
object ZMQ {

  /**
   * Creates a composite version number
   * @param major the major version of 0MQ
   * @param minor the minor version of 0MQ
   * @param patch the patch version of 0MQ
   * @return major * 10000 + minor * 100 + patch
   */
  def makeVersion(major: Int, minor: Int, patch: Int): Int = major * 10000 + minor * 100 + patch

  /**
   * Creates a new 0MQ Context with the specified number of IO Threads
   * @param ioThreads the number of ioThreads the Context should have
   * @return a newly created Context
   */
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

  /**
   * Represents a 0MQ Context
   * @param ioThreads the number of IO Threads this Context should have
   */
  class Context(ioThreads: Int) {
    protected[zeromq] final val ptr: Pointer = zmq.zmq_init(ioThreads)

    /**
     * Terminates this Context
     */
    def term(): Unit = zmq.zmq_term(ptr)

    /**
     * Creates a new Socket in this Context with the given Socket Type
     * @param `type` the type of Socket, see: PAIR, PUB, SUB, REQ, REP, DEALER, ROUTER, PULL, PUSH, XPUB, XSUB
     * @return a newly created Socket of the given type
     */
    def socket(`type`: Int): Socket = new Socket(this, `type`)

    /**
     * Creates a new Poller for this Context with default size of Sockets, 32
     * @return a newly created Poller with the given size
     */
    def poller(): Poller = poller(32)

    /**
     * Creates a new Poller for this Context
     * @return a newly created Poller with the given size
     */
    def poller(size: Int): Poller = new Poller(this, size)

    def setMaxSockets(maxSockets: Int): Boolean = zmq.zmq_ctx_set(ptr, ZeroMQ.ZMQ_MAX_SOCKETS, maxSockets)
  }

  private final val versionBelow210 = fullVersion < makeVersion(2, 1, 0)
  private final val versionAtleast210 = !versionBelow210
  private final val versionBelow220 = fullVersion < makeVersion(2, 2, 0)
  private final val versionAtleast220 = !versionBelow220
  private final val versionBelow300 = fullVersion < makeVersion(3, 0, 0)
  private final val versionAtleast300 = !versionBelow300

  /**
   * Represents a 0MQ Socket
   * @param context which Context the Socket belongs to
   * @param `type` the Socket Type (http://api.zeromq.org/2-1:zmq-socket)
   */
  class Socket(context: ZMQ.Context, `type`: Int) {
    import ZeroMQ._

    private[zeromq] final val ptr: Pointer = zmq.zmq_socket(context.ptr, `type`)
    private final val messageDataBuffer = new JHashSet[Pointer] with zmq_free_fn {
      override def invoke(data: Pointer, memory: Pointer): Unit = remove(memory)
    }

    /**
     * Closes this 0MQ Socket
     */
    def close(): Unit = zmq.zmq_close(ptr)

    /**
     * Retrieves the Socket Type
     * @return -1 if version < 2.1, or the Socket Type
     */
    def getType(): Int = if (versionBelow210) -1 else getLongSockopt(ZMQ_TYPE).asInstanceOf[Int]

    /**
     * Retrieves the Linger value for this Socket
     * @return -1 if version < 2.1, or the Linger Value
     */
    def getLinger(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_LINGER)

    /**
     * Retrieves the Reconnect Interval for this Socket
     * @return -1 if version < 2.1, or the Reconnect Interval
     */
    def getReconnectIVL(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_RECONNECT_IVL)

    /**
     * Retrieves the Backlog number for this Socket
     * @return -1 if version < 2.1, or the Backlog number
     */
    def getBacklog(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_BACKLOG)

    /**
     * Retrieves the Maximum Reconnect Interval for this Socket
     * @return -1 if version < 2.1, or the Maximum Reconnect Interval
     */
    def getReconnectIVLMax(): Int = if (versionBelow210) -1 else getIntSockopt(ZMQ_RECONNECT_IVL_MAX)

    /**
     * Retrieves the Maximum Message Size for this Socket
     * @return -1 if version < 3.0, or the Maximum Message Size
     */
    def getMaxMsgSize(): Long = if (versionBelow300) -1 else getLongSockopt(ZMQ_MAXMSGSIZE)

    /**
     * Retrieves the Send High Water Mark for this Socket
     * @return -1 if version < 3.0, or the Send High Water Mark
     */
    def getSndHWM(): Int = if (versionBelow300) -1 else getIntSockopt(ZMQ_SNDHWM)

    /**
     * Retrieves the Receive High Water Mark for this Socket
     * @return -1 if version < 3.0, or the Receive High Water Mark
     */
    def getRcvHWM(): Int = if (versionBelow300) -1 else getIntSockopt(ZMQ_RCVHWM)

    /**
     * Retrieves the High Water Mark for this Socket
     * @return -1 if version < 3.0, or the High Water Mark
     */
    def getHWM(): Long = if (versionBelow300) getLongSockopt(ZMQ_HWM) else -1

    /**
     * Retrieves the Swap size in bytes for this Socket
     * @return -1 if version < 3.0, or the Swap size in bytes
     */
    def getSwap(): Long = if (versionBelow300) -1 else getLongSockopt(ZMQ_SWAP)

    /**
     * Retrieves the Affinity for this Socket
     * @return the Affinity
     */
    def getAffinity(): Long = getLongSockopt(ZMQ_AFFINITY)

    /**
     * Retrieves the Identity of this Socket
     * @return the Identity of this Socket
     */
    def getIdentity(): Array[Byte] = getBytesSockopt(ZMQ_IDENTITY)

    /**
     * Retrieves the multicast data rate for this Socket
     * @return the multicast data rate
     */
    def getRate(): Long = getLongSockopt(ZMQ_RATE)

    /**
     * Retrieves the Recovery Interval for this Socket
     * @return the Recovery Interval in seconds
     */
    def getRecoveryInterval(): Long = getLongSockopt(ZMQ_RECOVERY_IVL)

    /**
     * Retrieves whether this Socket has Multicast Loop enabled
     * @return false if version >= 3.0, or whether this Socket has Multicast Loop enabled
     */
    def hasMulticastLoop(): Boolean = if (versionAtleast300) false else getLongSockopt(ZMQ_MCAST_LOOP) != 0

    /**
     * Sets the maximum number of hops for multicast messages
     * @param mcast_hops the maximum number of hops
     */
    def setMulticastHops(mcast_hops: Long): Unit = setLongSockopt(ZMQ_MCAST_LOOP, mcast_hops)

    /**
     * Retrieves the maximum number of hops for multicast messages for this Socket
     * @return -1 if version < 3.0, or the maximum number of hops for multicast messages
     */
    def getMulticastHops(): Long = if (versionBelow300) -1 else getLongSockopt(ZMQ_MCAST_LOOP)

    /**
     * Sets the Receive Timeout for this Socket, if the 0MQ version is at least 2.2
     * @param timeout in millis, -1 for infinity and 0 for no timeout
     */
    def setReceiveTimeOut(timeout: Int): Unit = if (versionAtleast220) setIntSockopt(ZMQ_RCVTIMEO, timeout)

    /**
     * Retrieves the Receive Timeout for this Socket
     * @return -1 if version < 2.2, or the Receive Timeout
     */
    def getReceiveTimeOut(): Int = if (versionBelow220) -1 else getIntSockopt(ZMQ_RCVTIMEO)

    /**
     * Sets the Send Timeout for this Socket, if the 0MQ version is at least 2.2
     * @param timeout in milliseconds, -1 for infinity and 0 for no timeout
     */
    def setSendTimeOut(timeout: Int): Unit = if (versionAtleast220) setIntSockopt(ZMQ_SNDTIMEO, timeout)

    /**
     * Retrieves the Send Timeout for this Socket
     * @return -1 if version < 2.2, or the Send Timeout
     */
    def getSendTimeOut(): Int = if (versionBelow220) -1 else getIntSockopt(ZMQ_SNDTIMEO)

    /**
     * Retrieves the Send Buffer Size for this Socket
     * @return the Send Buffer Size
     */
    def getSendBufferSize(): Long = getLongSockopt(ZMQ_SNDBUF)

    /**
     * Sets the Send Buffer Size for this Socket
     * @param sndbuf size in bytes
     */
    def setSendBufferSize(sndbuf: Long): Unit = setLongSockopt(ZMQ_SNDBUF, sndbuf)

    /**
     * Retrieves the Receive Buffer Size for this Socket
     * @return the Receive Buffer Size
     */
    def getReceiveBufferSize(): Long = getLongSockopt(ZMQ_RCVBUF)

    /**
     * Retrieves whether the last message that was received was a partial message with more to follow
     * @return true if more data is to follow, false if not
     */
    def hasReceiveMore(): Boolean = getLongSockopt(ZMQ_RCVMORE) != 0

    /**
     * Retrieves the File Descriptor for this Socket
     * @return -1 if version < 2.1, or the File Descriptor
     */
    def getFD(): Long = if (versionBelow210) -1 else getLongSockopt(ZMQ_FD)

    /**
     * Retrieves the Event State for this Socket
     * @return -1 if version < 2.1, or a bit mask of ZMQ_POLLIN and ZMQ_POLLOUT depending if reading and/or writing is possible
     */
    def getEvents(): Long = if (versionBelow210) -1 else getLongSockopt(ZMQ_EVENTS)

    /**
     * Sets the Linger period if the 0MQ version is at least 2.1 for this Socket
     * @param linger the linger period in millis, 0 to indicate no linger
     */
    def setLinger(linger: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_LINGER, linger)

    /**
     * Sets the Reconnect Interval, if the 0MQ version is at least 2.1, for this Socket
     * @param reconnectIVL in milliseconds, -1 indicates no reconnection
     */
    def setReconnectIVL(reconnectIVL: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_RECONNECT_IVL, reconnectIVL)

    /**
     * Sets the Backlog of connections, if the 0MQ version is at least 2.1, for this Socket
     * @param backlog in number of connections
     */
    def setBacklog(backlog: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_BACKLOG, backlog)

    /**
     * Sets the Maximum Reconnect Interval, if the 0MQ version is at least 2.1, for this Socket
     * @param reconnectIVLMax in milliseconds, 0 for no backoff, values less than reconnectIVL will be ignored
     */
    def setReconnectIVLMax(reconnectIVLMax: Int): Unit = if (versionAtleast210) setIntSockopt(ZMQ_RECONNECT_IVL_MAX, reconnectIVLMax)

    /**
     * Sets the Maximum Message Size, if the 0MQ version is at least 3.0, for this Socket
     * @param maxMsgSize in bytes, -1 for no limit
     */
    def setMaxMsgSize(maxMsgSize: Long): Unit = if (versionAtleast300) setLongSockopt(ZMQ_MAXMSGSIZE, maxMsgSize)

    /**
     * Sets the Send High Water Mark, if the 0MQ version is at least 3.0, for this Socket
     * @param sndHWM in number of messages
     */
    def setSndHWM(sndHWM: Int): Unit = if (versionAtleast300) setIntSockopt(ZMQ_SNDHWM, sndHWM)

    /**
     * Sets the Receive High Water Mark, if the 0MQ version is at least 3.0, for this Socket
     * @param rcvHWM in number of messages
     */
    def setRcvHWM(rcvHWM: Int): Unit = if (versionAtleast300) setIntSockopt(ZMQ_RCVHWM, rcvHWM)

    /**
     * Sets the High Water Mark, if the 0MQ version is < 3.0, for this Socket
     * @param hwm in number of messages, 0 means no limit
     */
    def setHWM(hwm: Long): Unit = if (versionBelow300) setLongSockopt(ZMQ_HWM, hwm)

    /**
     * Sets the Swap, if the 0MQ version is > 3.0, for this Socket
     * @param swap in number of bytes
     */
    def setSwap(swap: Long): Unit = if (versionAtleast300) setLongSockopt(ZMQ_SWAP, swap)

    /**
     * Sets the Affinity for this Socket
     * @param affinity a bit mask representing which IO Threads to assign affinity towards
     */
    def setAffinity(affinity: Long): Unit = setLongSockopt(ZMQ_AFFINITY, affinity)

    /**
     * Sets the Identity of this Socket
     * @param identity at least 1 byte and at most 255 bytes
     */
    def setIdentity(identity: Array[Byte]): Unit = setBytesSockopt(ZMQ_IDENTITY, identity)


    /**
     * Sets the Data Rate for multicast transports for this Socket
     * @param rate in kbits per second
     */
    def setRate(rate: Long): Unit = setLongSockopt(ZMQ_RATE, rate)

    /**
     * Sets the Recovery Interval for this Socket
     * @param recoveryIVL in seconds
     */
    def setRecoveryInterval(recoveryIVL: Long): Unit = setLongSockopt(ZMQ_RECOVERY_IVL, recoveryIVL)

    /**
     * Sets Multicast Loop, if 0MQ version < 3.0, for this Socket
     * @param mcast_loop true to enable, false to disable
     */
    def setMulticastLoop(mcast_loop: Boolean): Unit = if (versionBelow300) setLongSockopt(ZMQ_MCAST_LOOP, if (mcast_loop) 1 else 0)

    /**
     * Sets the Receive Buffer Size for this Socket
     * @param rcvbuf in bytes, 0 means use OS default
     */
    def setReceiveBufferSize(rcvbuf: Long): Unit = setLongSockopt(ZMQ_RCVBUF, rcvbuf)

    /**
     * Subscribes this Socket to a type of messages
     * @param topic empty array for all messages, non-empty array to prefix match inbound messages
     */
    def subscribe(topic: Array[Byte]): Unit = setBytesSockopt(ZMQ_SUBSCRIBE, topic)

    /**
     * Unsubscribes this Socket from a type of messages
     * @param topic empty array for all messages, non-empty array to prefix match inbound messages
     */
    def unsubscribe(topic: Array[Byte]): Unit = setBytesSockopt(ZMQ_UNSUBSCRIBE, topic)

    /**
     * Binds this Socket to an address
     * @param addr the address to bind to, according to: http://api.zeromq.org/2-1:zmq-bind
     */
    def bind(addr: String): Unit = {
      if (zmq.zmq_bind(ptr, addr) != 0) {
        val errno = zmq.zmq_errno
        throw new ZMQException(zmq.zmq_strerror(errno), errno)
      }
    }

    /**
     * Connects this Socket to an address
     * @param addr the address to connect to, according to: http://api.zeromq.org/2-1:zmq-connect
     */
    def connect(addr: String): Unit = {
      if (zmq.zmq_connect(ptr, addr) != 0) {
        val errno = zmq.zmq_errno
        throw new ZMQException(zmq.zmq_strerror(errno), errno)
      }
    }

    /**
     * Sends the given message to this Socket, see the following for more details: http://api.zeromq.org/2-1:zmq-send
     * @param msg the bytes to send
     * @param flags ZMQ_NOBLOCK or ZMQ_SNDMORE or both
     * @return true if send succeeded, false if NOBLOCK requested and EAGAIN returned by send call
     */
    def send(msg: Array[Byte], flags: Int): Boolean = {
      val message = newZmqMessage(msg)
      if (zmq.zmq_msg_send(message, ptr, flags) < 0) {
        zmq.zmq_msg_close(message)
        raiseZMQException()
      } else {
        if (zmq.zmq_msg_close(message) != 0) raiseZMQException() else false
      }
    }

    /**
     * Receives a message from this Socket, see the following for more details: http://api.zeromq.org/3-2:zmq-recv
     * @param flags
     * @return null if NOBLOCK was requested and EAGAIN was returned by recv call, else the bytes received
     */
    def recv(flags: Int): Array[Byte] = {
      val message = newZmqMessage()
      if (zmq.zmq_msg_recv(message, ptr, flags) < 0) {
        if (zmq.zmq_errno == EAGAIN) {
          if (zmq.zmq_msg_close(message) != 0) raiseZMQException() else null
        }
        else {
          zmq.zmq_msg_close(message)
          raiseZMQException()
        }
      } else {
        val dataByteArray = zmq.zmq_msg_data(message).getByteArray(0, zmq.zmq_msg_size(message))
        if (zmq.zmq_msg_close(message) != 0) raiseZMQException() else dataByteArray
      }
    }

    override protected def finalize: Unit = close()

    private def getLongSockopt(option: Int): Long = {
      val value = new Memory(JLong.SIZE / 8)
      val length = new LongByReference(JLong.SIZE / 8)
      zmq.zmq_getsockopt(ptr, option, value, length)
      value.getLong(0)
    }

    private def setLongSockopt(option: Int, optval: Long): Unit = {
      val value = new Memory(JLong.SIZE / 8)
      value.setLong(0, optval)
      zmq.zmq_setsockopt(ptr, option, value, new NativeLong(JLong.SIZE / 8))
    }

    private def getIntSockopt(option: Int): Int = {
      val value = new Memory(JInteger.SIZE / 8)
      zmq.zmq_getsockopt(ptr, option, value, new LongByReference(JInteger.SIZE / 8))
      value.getInt(0)
    }

    private def setIntSockopt(option: Int, optval: Int): Unit = {
      val value = new Memory(JInteger.SIZE / 8)
      value.setInt(0, optval)
      zmq.zmq_setsockopt(ptr, option, value, new NativeLong(JInteger.SIZE / 8))
    }

    private def getBytesSockopt(option: Int): Array[Byte] = {
      val value = new Memory(1024)
      val length = new LongByReference(1024)
      zmq.zmq_getsockopt(ptr, option, value, length)
      value.getByteArray(0, length.getValue.intValue)
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

    private def newZmqMessage(msg: Array[Byte] = null): zmq_msg_t = {
      val message = new zmq_msg_t
      if (msg eq null) {
        if (zmq.zmq_msg_init(message) != 0) raiseZMQException()
      } else {
        msg.length match {
          case 0 ⇒ if (zmq.zmq_msg_init_size(message, new NativeLong(0)) != 0) raiseZMQException()
          case len ⇒
            val mem = new Memory(len)
            mem.write(0, msg, 0, len)
            if (zmq.zmq_msg_init_data(message, mem, new NativeLong(len), messageDataBuffer, mem) != 0) raiseZMQException()
            else messageDataBuffer.add(mem)
        }
      }
      message
    }

    private def raiseZMQException(errno: Int = zmq.zmq_errno): Nothing = throw new ZMQException(zmq.zmq_strerror(errno), errno)
  }

  /**
   * A Poller is a constructs which makes it easy to which Sockets have inbound or outbound messages pending
   * @param context the 0MQ context this Poller belongs to
   * @param size the initial size of the Poller, in number of Sockets
   */
  class Poller (context: ZMQ.Context, size: Int) {
    @BeanProperty var timeout: FiniteDuration = Duration(-1, "ms")
    private var nextEventIndex: Int = 0
    private var maxEventCount: Int = size
    private var curEventCount: Int = 0
    private var sockets: Array[ZMQ.Socket] = new Array(size)
    private var events: Array[Short] = new Array(size)
    private var revents: Array[Short] = new Array(size)
    private var freeSlots: List[Int] = Nil

    /**
     * Registers the specified Socket to all of ZMQ_POLLIN, ZMQ_POLLOUT and ZMQ_POLLERR
     * @param socket the socket which to register to this Poller
     * @return the index of the registered socket
     */
    def register(socket: ZMQ.Socket): Int = register(socket, ZeroMQ.ZMQ_POLLIN | ZeroMQ.ZMQ_POLLOUT | ZeroMQ.ZMQ_POLLERR)

    /**
     * Registers the specified Socket to the specified types of Events
     * @param socket the socket which to register to this Poller
     * @return the index of the registered socket
     */
    def register(socket: ZMQ.Socket, numEvents: Int): Int = {
      require(numEvents <= Short.MaxValue, "numEvents must be less or equal to Short.MaxValue")
      require(numEvents >= Short.MinValue, "numEvents must be greater or equal to Short.MinValue")
      //FIXME handle the case where Socket is already registered
      val pos: Int = freeSlots match {
        case Nil ⇒
          if (nextEventIndex >= maxEventCount) {
            val newMaxEventCount: Int = maxEventCount + 16
            
            if (newMaxEventCount > Short.MaxValue) throw new IllegalStateException("maxEventCount may not grow past Short.MaxValue!")
            
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

    /**
     * Unregisters the specified Socket from this Poller
     * @param socket the socket which to unregister from this Poller
     */
    def unregister(socket: ZMQ.Socket): Unit = if (socket ne null) {
      @tailrec def unreg(index: Int): Boolean =
        if (index >= nextEventIndex || index >= sockets.length) false
        else if(sockets(index) eq socket) {
          sockets(index) = null
          events(index) = 0: Short
          revents(index) = 0: Short
          freeSlots ::= index
          curEventCount -= 1
          true
        } else unreg(index + 1)
      
      unreg(0)
    }

    /**
     * a means to obtain the Socket at a given index
     * @param index the index for the Socket
     * @return the Socket at the given index, non-existing indices or absence of Socket at given index will yield null
     */
    def getSocket(index: Int): ZMQ.Socket = if ((index < 0 || index >= nextEventIndex)) null else sockets(index)

    /**
     * Returns the current max size of this Poller
     */
    def getSize(): Int = maxEventCount

    /**
     * Returns the index to the next event
     */
    def getNext(): Int = nextEventIndex

    /**
     * Polls the registered Sockets using the current timeout of this Poller
     * @return how many items during the poll that yielded revents
     */
    def poll(): Long = poll(this.timeout)

    /**
     * Polls the registered Sockets using a speficied timeout
     * @param timeout the timeout for the poll operation
     * @return how many items during the poll that yielded revents
     */
    def poll(timeout: FiniteDuration): Long = {
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
                val item = items(itemIndex)
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
              val result: Int = zmq.zmq_poll(
                items,
                expectedEvents,
                new NativeLong(if (versionAtleast300) timeout.toMicros else timeout.toMillis)
              )
              withItems(items, init = false)
              result
            case _ ⇒ 0 // Bail out
          }
      }
    }

    private def poll_mask(index: Int, mask: Int): Boolean =
      if ((mask <= 0 || index < 0 || index >= nextEventIndex)) false else (revents(index) & mask) > 0

    /**
     * Returns whether there are any ZMQ_POLLIN events to consume
     * @param index
     * @return true if ZMQ_POLLIN is set for revents at the given index, false if not
     */
    def pollin(index: Int): Boolean = poll_mask(index, ZeroMQ.ZMQ_POLLIN)

    /**
     * Returns whether there are any ZMQ_POLLOUT events to consume
     * @param index
     * @return true if ZMQ_POLLOUT is set for revents at the given index, false if not
     */
    def pollout(index: Int): Boolean = poll_mask(index, ZeroMQ.ZMQ_POLLOUT)

    /**
     * Returns whether there are any ZMQ_POLLERR events to consume
     * @param index
     * @return true if ZMQ_POLLERR is set for revents at the given index, false if not
     */
    def pollerr(index: Int): Boolean = poll_mask(index, ZeroMQ.ZMQ_POLLERR)
  }
}


