/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;


/**
 * Client-side, network layer, unchecked exception.
 */
@SuppressWarnings("serial")
public class NetworkException extends TimeChainException {

  public NetworkException() {  }

  public NetworkException(String message) {
    super(message);
  }

  public NetworkException(Throwable cause) {
    super(cause);
  }

  public NetworkException(String message, Throwable cause) {
    super(message, cause);
  }

  public NetworkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}

