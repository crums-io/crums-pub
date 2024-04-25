/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.except;

/**
 * A problem with a notary file.
 */
@SuppressWarnings("serial")
public class NotaryFileException extends NotaryException {

  public NotaryFileException() {
  }

  public NotaryFileException(String message) {
    super(message);
  }

  public NotaryFileException(Throwable cause) {
    super(cause);
  }

  public NotaryFileException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotaryFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
