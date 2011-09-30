package org.zeromq;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

public class zmq_msg_t extends Structure {
  public Pointer content;
  public byte flags;
  public byte vsm_size;
  public byte[] vsm_data = new byte[ZMQ_MAX_VSM_SIZE];
  private static final int ZMQ_MAX_VSM_SIZE = org.zeromq.ZeroMQ$.MODULE$.ZMQ_MAX_VSM_SIZE();
}
