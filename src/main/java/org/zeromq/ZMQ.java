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
package org.zeromq;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Offers an API similar to that of jzmq [1] written by Gonzalo Diethelm.
 * <p/>
 * 1. https://github.com/zeromq/jzmq
 */
public class ZMQ {
    private static final ZeroMQ$ Const = ZeroMQ$.MODULE$;
    private static final ZeroMQLibrary zmq = Const.loadLibrary();
    
    private static final int majorVersion;
    private static final int minorVersion;
    private static final int patchVersion;

    private static final int fullVersion;
    private static final String versionString;

    public static final int NOBLOCK = Const.ZMQ_NOBLOCK();
    public static final int DONTWAIT = Const.ZMQ_NOBLOCK();
    public static final int PAIR = Const.ZMQ_PAIR();
    public static final int SNDMORE = Const.ZMQ_SNDMORE();
    public static final int PUB = Const.ZMQ_PUB();
    public static final int SUB = Const.ZMQ_SUB();
    public static final int REQ = Const.ZMQ_REQ();
    public static final int REP = Const.ZMQ_REP();
    public static final int XREQ = Const.ZMQ_DEALER();
    public static final int XREP = Const.ZMQ_ROUTER();
    public static final int DEALER = Const.ZMQ_DEALER();
    public static final int ROUTER = Const.ZMQ_ROUTER();
    public static final int PULL = Const.ZMQ_PULL();
    public static final int PUSH = Const.ZMQ_PUSH();
    public static final int STREAMER = Const.ZMQ_STREAMER();
    public static final int FORWARDER = Const.ZMQ_FORWARDER();
    public static final int QUEUE = Const.ZMQ_QUEUE();

    static {
        final int[] _majorVersion = new int[1];
        final int[] _minorVersion = new int[1];
        final int[] _patchVersion = new int[1];
        zmq.zmq_version(_majorVersion, _minorVersion, _patchVersion);
        majorVersion = _majorVersion[0];
        minorVersion = _minorVersion[0];
        patchVersion = _patchVersion[0];

        fullVersion = makeVersion(majorVersion, minorVersion, patchVersion);
        versionString = String.format("%d.%d.%d", majorVersion, minorVersion, patchVersion);
    }

    public static int makeVersion(final int major, final int minor, final int patch) {
        return major * 10000 + minor * 100 + patch;
    }

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static int getPatchVersion() {
        return patchVersion;
    }

    public static int getFullVersion() {
        return fullVersion;
    }

    public static String getVersionString() {
        return versionString;
    }

    public static Context context(final int ioThreads) {
        return new Context(ioThreads);
    }

    public static class Context {
        protected final Pointer ptr;

        protected Context(final int ioThreads) {
            ptr = zmq.zmq_init(ioThreads);
        }

        public void term() {
            zmq.zmq_term(ptr);
        }

        public Socket socket(final int type) {
            return new Socket(this, type);
        }

        public Poller poller() {
            return new Poller(this);
        }

        public Poller poller(final int size) {
            return new Poller(this, size);
        }
    }

    public static class Socket {
        private final static boolean versionBelow210 = getFullVersion() < makeVersion(2, 1, 0);
        private final static boolean versionBelow220 = getFullVersion() < makeVersion(2, 2, 0);
        private final static boolean versionBelow300 = getFullVersion() < makeVersion(3, 0, 0);

        protected final Pointer ptr;
        final MessageDataBuffer messageDataBuffer = new MessageDataBuffer();

        public void close() {
            zmq.zmq_close(ptr);
        }

        public int getType() {
            return (versionBelow210) ? -1 : (int) getLongSockopt(Const.ZMQ_TYPE());
        }

        public int getLinger() {
            return (versionBelow210) ? -1 : getIntSockopt(Const.ZMQ_LINGER());
        }

        public int getReconnectIVL() {
            return (versionBelow210) ? -1 : getIntSockopt(Const.ZMQ_RECONNECT_IVL());
        }

        public int getBacklog() {
            return (versionBelow210) ? -1 : getIntSockopt(Const.ZMQ_BACKLOG());
        }

        public int getReconnectIVLMax() {
            return (versionBelow210) ? -1 : getIntSockopt(Const.ZMQ_RECONNECT_IVL_MAX());
        }

        public long getMaxMsgSize() {
            return (versionBelow300) ? -1 : getLongSockopt(Const.ZMQ_MAXMSGSIZE());
        }

        public int getSndHWM() {
            return (versionBelow300) ? -1 : getIntSockopt(Const.ZMQ_SNDHWM());
        }

        public int getRcvHWM() {
            return (versionBelow300) ? -1 : getIntSockopt(Const.ZMQ_RCVHWM());
        }

        public long getHWM() {
            return (versionBelow300) ? getLongSockopt(Const.ZMQ_HWM()) : -1;
        }

        public long getSwap() {
            return (versionBelow300) ? -1 : getLongSockopt(Const.ZMQ_SWAP());
        }

        public long getAffinity() {
            return getLongSockopt(Const.ZMQ_AFFINITY());
        }

        public byte[] getIdentity() {
            return getBytesSockopt(Const.ZMQ_IDENTITY());
        }

        public long getRate() {
            return getLongSockopt(Const.ZMQ_RATE());
        }

        public long getRecoveryInterval() {
            return getLongSockopt(Const.ZMQ_RECOVERY_IVL());
        }

        public boolean hasMulticastLoop() {
            return (versionBelow300) ? false : getLongSockopt(Const.ZMQ_MCAST_LOOP()) != 0;
        }

        public void setMulticastHops(final long mcast_hops) {
            setLongSockopt(Const.ZMQ_MCAST_LOOP(), mcast_hops);
        }

        public long getMulticastHops() {
            return (versionBelow300) ? -1 : getLongSockopt(Const.ZMQ_MCAST_LOOP());
        }

        public void setReceiveTimeOut(final int timeout) {
            if (versionBelow220 == false) setIntSockopt(Const.ZMQ_RCVTIMEO(), timeout);
        }

        public int getReceiveTimeOut() {
            return (versionBelow220) ? -1 : getIntSockopt(Const.ZMQ_RCVTIMEO());
        }

        public void setSendTimeOut(final int timeout) {
            if (versionBelow220 == false) setIntSockopt(Const.ZMQ_SNDTIMEO(), timeout);
        }

        public int getSendTimeOut() {
            return (versionBelow220) ? -1 : getIntSockopt(Const.ZMQ_SNDTIMEO());
        }

        public long getSendBufferSize() {
            return getLongSockopt(Const.ZMQ_SNDBUF());
        }

        public long getReceiveBufferSize() {
            return getLongSockopt(Const.ZMQ_RCVBUF());
        }

        public boolean hasReceiveMore() {
            return getLongSockopt(Const.ZMQ_RCVMORE()) != 0;
        }

        public long getFD() {
            return (versionBelow210) ? -1 : getLongSockopt(Const.ZMQ_FD());
        }

        public long getEvents() {
            return (versionBelow210) ? -1 : getLongSockopt(Const.ZMQ_EVENTS());
        }

        public void setLinger(final int linger) {
            if (versionBelow210 == false) setIntSockopt(Const.ZMQ_LINGER(), linger);
        }

        public void setReconnectIVL(final int reconnectIVL) {
            if (versionBelow210 == false) setIntSockopt(Const.ZMQ_RECONNECT_IVL(), reconnectIVL);
        }

        public void setBacklog(final int backlog) {
            if (versionBelow210 == false) setIntSockopt(Const.ZMQ_BACKLOG(), backlog);
        }

        public void setReconnectIVLMax(final int reconnectIVLMax) {
            if (versionBelow210 == false) setIntSockopt(Const.ZMQ_RECONNECT_IVL_MAX(), reconnectIVLMax);
        }

        public void setMaxMsgSize(final long maxMsgSize) {
            if (versionBelow300 == false) setLongSockopt(Const.ZMQ_MAXMSGSIZE(), maxMsgSize);
        }

        public void setSndHWM(final int sndHWM) {
            if (versionBelow300 == false) setIntSockopt(Const.ZMQ_SNDHWM(), sndHWM);
        }

        public void setRcvHWM(final int rcvHWM) {
            if (versionBelow300) setIntSockopt(Const.ZMQ_RCVHWM(), rcvHWM);
        }

        public void setHWM(final long hwm) {
            if (versionBelow300) setLongSockopt(Const.ZMQ_HWM(), hwm);
        }

        public void setSwap(final long swap) {
            if (versionBelow300 == false) setLongSockopt(Const.ZMQ_SWAP(), swap);
        }

        public void setAffinity(final long affinity) {
            setLongSockopt(Const.ZMQ_AFFINITY(), affinity);
        }

        public void setIdentity(final byte[] identity) {
            setBytesSockopt(Const.ZMQ_IDENTITY(), identity);
        }

        public void subscribe(final byte[] topic) {
            setBytesSockopt(Const.ZMQ_SUBSCRIBE(), topic);
        }

        public void unsubscribe(final byte[] topic) {
            setBytesSockopt(Const.ZMQ_UNSUBSCRIBE(), topic);
        }

        public void setRate(final long rate) {
            setLongSockopt(Const.ZMQ_RATE(), rate);
        }

        public void setRecoveryInterval(final long recovery_ivl) {
            setLongSockopt(Const.ZMQ_RECONNECT_IVL(), recovery_ivl);
        }

        public void setMulticastLoop(final boolean mcast_loop) {
            if (versionBelow300) setLongSockopt(Const.ZMQ_MCAST_LOOP(), mcast_loop ? 1 : 0);
        }

        public void setSendBufferSize(final long sndbuf) {
            setLongSockopt(Const.ZMQ_SNDBUF(), sndbuf);
        }

        public void setReceiveBufferSize(final long rcvbuf) {
            setLongSockopt(Const.ZMQ_RCVBUF(), rcvbuf);
        }

        public void bind(final String addr) {
            zmq.zmq_bind(ptr, addr);
        }

        public void connect(final String addr) {
            zmq.zmq_connect(ptr, addr);
        }

        public boolean send(final byte[] msg, final int flags) {
            final zmq_msg_t message = newZmqMessage(msg);
            boolean wasSent = true;
            if (zmq.zmq_send(ptr, message, flags) != 0) { // problem sending
                if (zmq.zmq_errno() == Const.EAGAIN()) {
                    wasSent = false;
                } else {
                    zmq.zmq_msg_close(message);
                    raiseZMQException();
                }
            }

            if (zmq.zmq_msg_close(message) != 0)
                raiseZMQException();

            return wasSent;
        }

        public byte[] recv(final int flags) {
            final zmq_msg_t message = newZmqMessage();
            if (zmq.zmq_recv(ptr, message, flags) != 0) {
                if (zmq.zmq_errno() == Const.EAGAIN()) {
                    if (zmq.zmq_msg_close(message) != 0) {
                        raiseZMQException();
                    } else {
                        return null;
                    }
                } else {
                    zmq.zmq_msg_close(message);
                    raiseZMQException();
                }
            }
            final Pointer data = zmq.zmq_msg_data(message);
            final int length = zmq.zmq_msg_size(message);
            final byte[] dataByteArray = data.getByteArray(0, length);
            if (zmq.zmq_msg_close(message) != 0)
                raiseZMQException();

            return dataByteArray;
        }

        protected Socket(final Context context, final int type) {
            ptr = zmq.zmq_socket(context.ptr, type);
        }

        @Override
        protected void finalize() {
            close();
        }

        private long getLongSockopt(final int option) {
            final Memory value = new Memory(Long.SIZE / 8);
            final LongByReference length = new LongByReference(Long.SIZE / 8);
            zmq.zmq_getsockopt(ptr, option, value, length);
            return value.getLong(0);
        }

        private void setLongSockopt(final int option, final long optval) {
            final NativeLong length = new NativeLong(Long.SIZE / 8);
            final Memory value = new Memory(Long.SIZE / 8);
            value.setLong(0, optval);
            zmq.zmq_setsockopt(ptr, option, value, length);
        }

        private int getIntSockopt(final int option) {
            final Memory value = new Memory(Integer.SIZE / 8);
            final LongByReference length = new LongByReference(Integer.SIZE / 8);
            zmq.zmq_getsockopt(ptr, option, value, length);
            return value.getInt(0);
        }

        private void setIntSockopt(final int option, final int optval) {
            final NativeLong length = new NativeLong(Integer.SIZE / 8);
            final Memory value = new Memory(Integer.SIZE / 8);
            value.setInt(0, (int) optval);
            zmq.zmq_setsockopt(ptr, option, value, length);
        }

        private byte[] getBytesSockopt(final int option) {
            final Memory value = new Memory(1024);
            final LongByReference length = new LongByReference(1024);
            zmq.zmq_getsockopt(ptr, option, value, length);
            return value.getByteArray(0, (int) length.getValue());
        }

        private void setBytesSockopt(final int option, final byte[] optval) {
            final Pointer value;
            if (optval.length > 0) {
                value = new Memory(optval.length);
                value.write(0, optval, 0, optval.length);
            } else {
                value = Pointer.NULL;
            }
            zmq.zmq_setsockopt(ptr, option, value, new NativeLong(optval.length));
        }

        private zmq_msg_t newZmqMessage(final byte[] msg) {
            final zmq_msg_t message = new zmq_msg_t();
            if (msg.length == 0) {
                if (zmq.zmq_msg_init_size(message, new NativeLong(msg.length)) != 0) {
                    raiseZMQException();
                }
            } else {
                final Memory mem = new Memory(msg.length);
                mem.write(0, msg, 0, msg.length);
                if (zmq.zmq_msg_init_data(message, mem, new NativeLong(msg.length), messageDataBuffer, mem) == 0) {
                    messageDataBuffer.add(mem);
                } else {
                    raiseZMQException();
                }
            }
            return message;
        }

        private zmq_msg_t newZmqMessage() {
            final zmq_msg_t message = new zmq_msg_t();
            if (zmq.zmq_msg_init(message) != 0)
                raiseZMQException();
            return message;
        }

        private void raiseZMQException() {
            final int errno = zmq.zmq_errno();
            final String reason = zmq.zmq_strerror(errno);
            throw new ZMQException(reason, errno);
        }

        private final class MessageDataBuffer implements zmq_free_fn {
            private final HashSet<Pointer> buffer = new HashSet<Pointer>();

            public synchronized void add(final Pointer data) {
                buffer.add(data);
            }

            public synchronized void invoke(final Pointer data, final Pointer memory) {
                buffer.remove(memory);
            }
        }
    }

    public static class Poller {
        public static final int POLLIN = Const.ZMQ_POLLIN();
        public static final int POLLOUT = Const.ZMQ_POLLOUT();
        public static final int POLLERR = Const.ZMQ_POLLERR();

        private static final int SIZE_DEFAULT = 32;
        private static final int SIZE_INCREMENT = 16;

        private long timeout = -1;
        private int nextEventIndex = 0;
        private int maxEventCount = 0;
        private int curEventCount = 0;
        private Socket[] sockets = null;
        private short[] events = null;
        private short[] revents = null;
        private final Queue<Integer> freeSlots = new LinkedList<Integer>();

        protected Poller(final Context context) {
            this(context, SIZE_DEFAULT);
        }

        protected Poller(final Context context, final int size) {
            this.maxEventCount = size;
            this.sockets = new Socket[size];
            this.events = new short[size];
            this.revents = new short[size];
        }

        public int register(final Socket socket) {
            return register(socket, POLLIN | POLLOUT | POLLERR);
        }

        public int register(final Socket socket, final int numEvents) {
            final int pos;
            final Integer freePos = freeSlots.poll();
            if (freePos != null) {
                pos = freePos;
            } else {
                if (nextEventIndex >= maxEventCount) {
                    final int newMaxEventCount = maxEventCount + SIZE_INCREMENT;
                    sockets = Arrays.copyOf(sockets, newMaxEventCount);
                    events = Arrays.copyOf(events, newMaxEventCount);
                    revents = Arrays.copyOf(revents, newMaxEventCount);
                    maxEventCount = newMaxEventCount;
                }
                pos = nextEventIndex++;
            }
            sockets[pos] = socket;
            events[pos] = (short) numEvents;
            ++curEventCount;
            return pos;
        }

        public void unregister(final Socket socket) {
            for (int index = 0; index < nextEventIndex; index++) {
                if (sockets[index] == socket) {
                    sockets[index] = null;
                    events[index] = 0;
                    revents[index] = 0;
                    freeSlots.add(index);
                    --curEventCount;
                    break;
                }
            }
        }

        public Socket getSocket(final int index) {
            return (index < 0 || index >= nextEventIndex) ? null : sockets[index];
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(final long timeout) {
            this.timeout = timeout;
        }

        public int getSize() {
            return maxEventCount;
        }

        public int getNext() {
            return nextEventIndex;
        }

        public long poll() {
            return poll(this.timeout);
        }

        public long poll(final long timeout) {
            Arrays.fill(revents, 0, nextEventIndex, (short) 0);
            if (curEventCount == 0)
                return 0;
            final zmq_pollitem_t[] items = (zmq_pollitem_t[]) new zmq_pollitem_t().toArray(curEventCount);
            {
                int itemIndex = 0;
                for (int socketIndex = 0; socketIndex < sockets.length; ++socketIndex) {
                    if (sockets[socketIndex] != null) {
                        final zmq_pollitem_t item = items[itemIndex];
                        item.socket = sockets[socketIndex].ptr;
                        item.fd = 0; // We've already set socket, no need to set fd
                        item.events = events[socketIndex];
                        item.revents = 0; // Clear, as specced: http://api.zeromq.org/2-1:zmq-poll
                        ++itemIndex;
                    }
                }

                if (itemIndex != curEventCount)
                    return 0;
            }

            int itemIndex = 0;
            final int result = zmq.zmq_poll(items, curEventCount, new NativeLong(timeout));
            for (int socketIndex = 0; socketIndex < sockets.length; socketIndex++) {
                if (sockets[socketIndex] != null) {
                    revents[socketIndex] = items[itemIndex].revents;
                    ++itemIndex;
                }
            }

            return result;
        }

        public boolean pollin(final int index) {
            return poll_mask(index, POLLIN);
        }

        public boolean pollout(final int index) {
            return poll_mask(index, POLLOUT);
        }

        public boolean pollerr(final int index) {
            return poll_mask(index, POLLERR);
        }

        private boolean poll_mask(final int index, final int mask) {
            return (mask <= 0 || index < 0 || index >= nextEventIndex) ? false : (revents[index] & mask) > 0;
        }
    }
}
