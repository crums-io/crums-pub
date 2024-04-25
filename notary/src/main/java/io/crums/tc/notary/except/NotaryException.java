/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.except;

import io.crums.tc.except.TimeChainException;
import io.crums.tc.notary.Notary;

/**
 * Thrown by {@linkplain Notary}.
 */
@SuppressWarnings("serial")
public class NotaryException extends TimeChainException {

  public NotaryException() {  }

  public NotaryException(String message) {
    super(message);
  }

  public NotaryException(Throwable cause) {
    super(cause);
  }

  public NotaryException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotaryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
