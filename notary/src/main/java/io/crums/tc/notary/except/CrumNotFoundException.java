/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.except;

/**
 * 
 */
@SuppressWarnings("serial")
public class CrumNotFoundException extends NotaryException {

  public CrumNotFoundException() {
  }

  public CrumNotFoundException(String message) {
    super(message);
  }

  public CrumNotFoundException(Throwable cause) {
    super(cause);
  }

  public CrumNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public CrumNotFoundException(
      String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
