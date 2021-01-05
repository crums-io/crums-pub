/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.client;

import io.crums.model.CrumsException;

/**
 * Client-side <tt>CrumsException</tt>. Typically wraps (caused by) a lower level exception
 * such as from I/O, parsing, etc.
 */
@SuppressWarnings("serial")
public class ClientException extends CrumsException {

  public ClientException(String message) {
    super(message);
  }

  public ClientException(Throwable cause) {
    super(cause);
  }

  public ClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public ClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
