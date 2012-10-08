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

import scala.util.Random
import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.MustMatchers
import org.zeromq.ZMQ._

trait AbstractZeroMQSpec extends WordSpec with MustMatchers with BeforeAndAfter {

  val zmq = ZeroMQ.loadLibrary

  val endpoint = "inproc://zeromq-spec"

  val subscribeAll = Array.empty[Byte]

  lazy val dataBytes = "hello".getBytes

  def bindTo = "tcp://127.0.0.1:" + 1024 + new Random(System.currentTimeMillis).nextInt(4096)

  def connectTestSubscriber(context: Context): ZMQ.Socket = {
    val socket = context.socket(ZMQ.SUB)
    socket.connect(endpoint)
    socket.subscribe(Array.empty)
    socket
  }

}
