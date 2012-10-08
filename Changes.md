# Changes

## 3.2 Upgrade API Changes
See http://www.zeromq.org/docs:3-1-upgrade for documented ZeroMQ modifications, removals, additions.

The following changes were tested against ZeroMQ 2.2.0 adn 3.2.0 on MacOSX 10.7.5. Further
testing on a other operating systems is needed.

### Modifications
Product version changed to 1.0.0-SNAPSHOT as an attempt to not collide with < 3.2.0 binding upgrades.

The scala version has been upgraded to 2.10.0-M7

The sbt version has been upgraded to 0.12.0

Six method return types have been modified to expose the zmq lib `int` success/failure, which
were not exposed in previous versions:

org.zeromq.Socket.java [migrated from `void` => `int`]:
````
int destroy()

int bind(String addr)

int connect(String addr)

int subscribe(byte[] topic)

int unsubscribe(byte[] topic)

int setBytesSockopt(int option, byte[] optval)

````

Copyright updated to 2012

### Additions
Added sbtscalariform plugin

org.zeromq.Context.scala
````
int destroy()
````

org.zeromq.ZeroMQLibrary.scala
````
def zmq_ctx_new: Pointer // creates with default thread count of 1
def zmq_ctx_set(socket: Pointer, name: Int, value: Int): Int
def zmq_msg_send(msg: zmq_msg_t, socket: Pointer, flags: Int): Int
def zmq_msg_recv(msg: zmq_msg_t, socket: Pointer, flags: Int): Int
def zmq_ctx_destroy(context: Pointer): Int

````

### Removals
org.zeromq.Context.java: unused & unimplemented

````
void term()
````