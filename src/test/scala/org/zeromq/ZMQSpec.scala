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
import org.scalatest.matchers.MustMatchers
import org.zeromq.ZMQ._

class ZMQSpec extends WordSpec with MustMatchers {
  "ZMQ" must {
    "support Socket#getType" in {
      val context = ZMQ.context(1)
      val sub = context.socket(ZMQ.SUB)
      sub.getType must equal(ZMQ.SUB)
      sub.close 
    }
    "support pub-sub connection pattern" in {
      val context = ZMQ.context(1)
      val (pub, sub, poller) = (
        context.socket(ZMQ.PUB), 
        context.socket(ZMQ.SUB), 
        context.poller
      )
      pub.bind("inproc://zmq-spec")
      sub.connect("inproc://zmq-spec")
      sub.subscribe(Array.empty)
      poller.register(sub)
      pub.send(outgoingMessage.getBytes, 0)
      poller.poll must equal(1)
      poller.pollin(0) must equal(true)
      val incomingMessage = sub.recv(0)
      incomingMessage must equal(outgoingMessage.getBytes)
      sub.close
      pub.close
    }
    "support polling of multiple sockets" in {
      val context = ZMQ.context(1)
      val (pub, poller) = (context.socket(ZMQ.PUB), context.poller)
      pub.bind("inproc://zmq-spec")
      val (sub_x, sub_y) = (connectSubscriber(context), connectSubscriber(context))
      poller.register(sub_x)
      poller.register(sub_y)
      pub.send(outgoingMessage.getBytes, 0)
      poller.poll must equal(2)
      poller.pollin(0) must equal(true)
      poller.pollin(1) must equal(true)
      sub_x.close
      sub_y.close
      pub.close
    }
    "support sending of zero-length messages" in {
      val context = ZMQ.context(1)
      val pub = context.socket(ZMQ.PUB)
      pub.send("".getBytes, 0)
      pub.close
    }
    "support setting timeout on receive and send" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val timeout = 123
      rep.setReceiveTimeOut(timeout)
      rep.setSendTimeOut(timeout)

      rep.bind(url)

      rep.getSendTimeOut must equal(timeout)
      rep.getReceiveTimeOut must equal(timeout)
    }
    "support setting the high water mark" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val hwq = 123
      rep.setHWM(hwq)

      rep.bind(url)

      rep.getHWM() must equal(hwq)
    }
    "support setting the rate" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val rate = 123
      rep.setRate(rate)

      rep.bind(url)

      rep.getRate() must equal(rate)
    }
    "support setting linger" in {
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val linger = 123
      rep.setLinger(linger)

      rep.bind(url)

      rep.getLinger() must equal(linger)
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

      rep.getReconnectIVL() must equal(rivl)
      rep.getReconnectIVLMax() must equal(rivlMax)
    }

    "support setting backlog" in { 
      val context = ZMQ.context(1)
      val rep = context.socket(ZMQ.REP)
      val url = "inproc://zmq-spec"
      val backlog = 123

      rep.setBacklog(backlog)
      rep.bind(url)

      rep.getBacklog() must equal(backlog)
    }
  }
  def connectSubscriber(context: Context) = {
    val socket = context.socket(ZMQ.SUB) 
    socket.connect("inproc://zmq-spec")
    socket.subscribe(Array.empty)
    socket
  }
  lazy val outgoingMessage = "hello"
}
