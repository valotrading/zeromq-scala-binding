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

import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.zeromq.ZMQ._

class ZMQSpec extends WordSpec with Matchers {
  "ZMQ" must {
    "support Socket#getType" in {
      val context = ZMQ.context(1)
      val sub = context.socket(ZMQ.SUB)
      sub.getType should equal(ZMQ.SUB)
      sub.close()
    }
    "raise error if connect fails" in {
      val context = ZMQ.context(1)
      val sub = context.socket(ZMQ.SUB)
      intercept[ZMQException] {
        sub.connect("inproc://zmq-spec")
      }
      sub.close()
    }
    "raise error if bind fails" in {
      val context = ZMQ.context(1)
      val sub = context.socket(ZMQ.SUB)
      intercept[ZMQException] {
        sub.bind("bad-address")
      }
      sub.close()
    }
    "support pub-sub connection pattern" in {
      val context = ZMQ.context(1)
      val (pub, sub, poller) = (context.socket(ZMQ.PUB),  context.socket(ZMQ.SUB),  context.poller)
      pub.bind("inproc://zmq-spec")
      sub.connect("inproc://zmq-spec")
      sub.subscribe(Array.empty)
      poller.register(sub)
      pub.send(outgoingMessage, 0)
      poller.poll should equal(1)
      poller.pollin(0) should equal(true)
      val incomingMessage = sub.recv(0)
      incomingMessage should equal(outgoingMessage)
      sub.close()
      pub.close()
    }
    "support polling of multiple sockets" in {
      val context = ZMQ.context(1)
      val (pub, poller) = (context.socket(ZMQ.PUB), context.poller)
      pub.bind("inproc://zmq-spec")
      val subs = 1 to 100 map { _ => connectSubscriber(context) }
      subs foreach poller.register
      pub.send(outgoingMessage, 0)
      poller.poll should equal(subs.size)
      subs.indices foreach { i => poller.pollin(i) should be(true) }
      subs foreach { _.recv(ZMQ.NOBLOCK) should equal(outgoingMessage) }
      subs foreach { _.close() }
      pub.close()
    }
    "support sending of zero-length messages" in {
      val context = ZMQ.context(1)
      val pub = context.socket(ZMQ.PUB)
      pub.send(Array.empty, 0)
      pub.close()
    }
    "support setting timeout on receive and send" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val timeout = 123
      rep.setReceiveTimeOut(timeout)
      rep.setSendTimeOut(timeout)

      rep.bind(url)

      rep.getSendTimeOut should equal(timeout)
      rep.getReceiveTimeOut should equal(timeout)
      rep.close()
    }
    "support setting the high water mark" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val hwq = 123
      rep.setHWM(hwq)

      rep.bind(url)

      rep.getHWM() should equal(hwq)
      rep.close()
    }
    "support setting the rate" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val rate = 123
      rep.setRate(rate)

      rep.bind(url)

      rep.getRate() should equal(rate)
      rep.close()
    }
    "support setting linger" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val linger = 123
      rep.setLinger(linger)

      rep.bind(url)

      rep.getLinger() should equal(linger)
      rep.close()
    }

    "support setting reconnect ivl" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val rivl = 123
      val rivlMax = 123

      rep.setReconnectIVL(rivl)
      rep.setReconnectIVLMax(rivlMax)
      rep.bind(url)

      rep.getReconnectIVL() should equal(rivl)
      rep.getReconnectIVLMax() should equal(rivlMax)
      rep.close()
    }

    "support setting backlog" in { 
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val backlog = 123

      rep.setBacklog(backlog)
      rep.bind(url)

      rep.getBacklog() should equal(backlog)
      rep.close()
    }
  }
  def connectSubscriber(context: Context) = {
    val socket = context.socket(ZMQ.SUB) 
    socket.connect("inproc://zmq-spec")
    socket.subscribe(Array.empty)
    socket
  }
  lazy val outgoingMessage = "hello".getBytes("UTF-8")
}
