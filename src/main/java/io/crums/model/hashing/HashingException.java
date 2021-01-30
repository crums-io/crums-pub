/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


/**
 * A hashing-related exception, usually as a result of parsing.
 */
@SuppressWarnings("serial")
public class HashingException extends RuntimeException {

  public HashingException() {  }

  public HashingException(String message) {
    super(message);
  }

  public HashingException(Throwable cause) {
    super(cause);
  }

  
  public HashingException(String message, Throwable cause) {
    super(message, cause);
  }


  public HashingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public synchronized HashingException fillInStackTrace() {
    super.fillInStackTrace();
    return this;
  }

}




