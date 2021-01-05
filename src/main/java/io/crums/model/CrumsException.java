/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


/**
 * Exception superclass.
 */
@SuppressWarnings("serial")
public abstract class CrumsException extends RuntimeException {

  public CrumsException(String message) {
    super(message);
  }

  public CrumsException(Throwable cause) {
    super(cause);
  }

  public CrumsException(String message, Throwable cause) {
    super(message, cause);
  }

  public CrumsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
