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
    "support Socket#getType" in {
      val context = ZMQ.context()
      val sub = context.socket(SUB)
      sub.getType must equal(SUB)
      sub.close
      context.destroy()
    }
    "support pub-sub connection pattern" in {
      val context = ZMQ.context()
      val (pub, sub, poller) = (
        context.socket(PUB),
        context.socket(SUB),
        context.poller)
      pub.bind(endpoint)
      sub.connect(endpoint)
      sub.subscribe(subscribeAll)
      poller.register(sub)
      pub.send(dataBytes, ZMQ_DONTWAIT)
      poller.poll must equal(1)
      poller.pollin(0) must equal(true)
      sub.recv(0) must equal(dataBytes)
      sub.close
      pub.close
      context.destroy()
    }
    "support polling of multiple sockets" in {
      val context = ZMQ.context()
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
      val context = ZMQ.context()
      val pub = context.socket(PUB)
      pub.bind(endpoint)
      pub.send("".getBytes, 0) must be(true)
      pub.close
      context.destroy()
    }
    "set socket linger" in {
      val context = ZMQ.context()
      val socket = context.socket(PUB)
      socket.setLinger(1000)
      socket.close
      context.destroy()
    }
  }
}
