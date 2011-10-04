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

class ZMQSpec extends WordSpec with MustMatchers {
  "ZMQ" must {
    "support pub-sub connection pattern" in {
      val context = ZMQ.context(1)
      val (pub, sub) = (context.socket(ZMQ.PUB), context.socket(ZMQ.SUB))
      pub.bind("inproc://zmq-spec")
      sub.connect("inproc://zmq-spec")
      sub.subscribe(Array.empty)
      pub.send(outgoingMessage.getBytes, 0)
      val incomingMessage = sub.recv(0)
      incomingMessage must equal(outgoingMessage.getBytes)
      sub.close
      pub.close
    }
  }
  lazy val outgoingMessage = "hello"
}
