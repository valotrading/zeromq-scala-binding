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

import org.zeromq.ZMQ._
import org.zeromq.ZeroMQ._

class ZMQSpec extends AbstractZeroMQSpec {
  "ZMQ" must {
    "be successfully created and destroyed" in {
      val context = new Context(1)
      context must not be (null)
      context.poller(1) must not be (null)
      val s = context.socket(PULL)
      s must not be (null)
      s.close
      context.destroy() must be(0)
    }
    "support Socket#getType" in {
      val context = new Context()
      val sub = context.socket(SUB)
      sub.getType must equal(SUB)
      sub.close
      context.destroy()
    }
    "support pub-sub connection pattern" in {
      val context = new Context()
      val (pub, sub, poller) = (
        context.socket(PUB),
        context.socket(SUB),
        context.poller)
      pub.bind(endpoint) must be(0)
      sub.connect(endpoint) must be(0)
      sub.subscribe(subscribeAll) must be(0)
      poller.register(sub) must be(0)
      pub.send(dataBytes, ZMQ_DONTWAIT)
      poller.poll must equal(1)
      poller.pollin(0) must equal(true)
      sub.recv(0) must equal(dataBytes)
      sub.close
      pub.close
      context.destroy()
    }
    "support polling of multiple sockets" in {
      val context = new Context()
      val (pub, poller) = (context.socket(PUB), context.poller)
      pub.bind(endpoint)
      val (sub_x, sub_y) = (connectTestSubscriber(context), connectTestSubscriber(context))
      poller.register(sub_x)
      poller.register(sub_y)
      pub.send(dataBytes, 0)
      poller.poll must equal(2)
      poller.pollin(0) must equal(true)
      poller.pollin(1) must equal(true)
      sub_x.close
      sub_y.close
      pub.close
      context.destroy()
    }
    "support sending of zero-length messages" in {
      val context = new Context()
      val pub = context.socket(PUB)
      pub.bind(endpoint)
      pub.send("".getBytes, 0) must be(true)
      pub.close
      context.destroy()
    }
    "support socket linger" in {
      val context = new Context()
      val socket = context.socket(PUB)
      socket.setLinger(1000)
      // FIXME fails: socket.getLinger must be(0)
      socket.close
      context.destroy()
    }
    "support socket backlog" in {
      val context = new Context()
      val socket = context.socket(REP)
      socket.setBacklog(200)
      socket.getBacklog must be(100)
      socket.close
      socket.connect("inproc://reqrep")
      socket.getBacklog must be(0)
      socket.close
      context.destroy()
    }
    "support socket reconnect interval" in {
      val context = new Context()
      val socket = context.socket(REP)
      socket.setReconnectIVL(101)
      socket.getReconnectIVL must be(100)
      socket.close
      socket.connect("inproc://reqrep")
      socket.getReconnectIVL must be(0)
      socket.close
      context.destroy()
    }
    /* TODO test coverage:
    getReconnectIVLMax
    getMaxMsgSize
    getSndHWM
    getRcvHWM
    getHWM
    getSwap
    getAffinity
    getIdentity
    getRate
    getRecoveryInterval
    getMulticastHops
    getReceiveTimeOut
    getSendTimeOut
    getSendBufferSize
    getReceiveBufferSize
    getFD
    getEvents

    hasMulticastLoop
    hasReceiveMore

    setReceiveTimeOut
    setMulticastHops
    setReceiveTimeOut
    setSendTimeOut  
    setReconnectIVL  
    setReconnectIVLMax 
    setMaxMsgSize 
    setSndHWM 
    setRcvHWM  
    setHWM(long hwm)  
    setSwap 
    setAffinity
    setIdentity
    setRate 
    setRecoveryInterval 
    setMulticastLoop 
    setSendBufferSize 
    setReceiveBufferSize
    */
  }
}
