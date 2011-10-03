package org.zeromq;

public class ZMQException extends RuntimeException {
  private int errorCode = 0;

  public ZMQException(String message, int errorCode) {
    super(message);

    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }
}
