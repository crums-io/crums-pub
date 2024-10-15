/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;


/**
 * Indicates a state or configuration error at a client-side repo.
 */
@SuppressWarnings("serial")
public class RepoException extends TimeChainException {

  public RepoException() {  }

  public RepoException(String message) {
    super(message);
  }

  public RepoException(Throwable cause) {
    super(cause);
  }

  public RepoException(String message, Throwable cause) {
    super(message, cause);
  }

  public RepoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
