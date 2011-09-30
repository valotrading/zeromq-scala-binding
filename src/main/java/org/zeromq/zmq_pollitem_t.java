package org.zeromq;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

public class zmq_pollitem_t extends Structure {
  public Pointer socket;
  public int fd;
  public short events;
  public short revents;
}
