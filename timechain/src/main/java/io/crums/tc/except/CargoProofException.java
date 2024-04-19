/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;

/**
 * 
 */
@SuppressWarnings("serial")
public class CargoProofException extends TimeChainException {

  public CargoProofException() {  }

  public CargoProofException(String message) {
    super(message);
  }

  public CargoProofException(Throwable cause) {
    super(cause);
  }

  public CargoProofException(String message, Throwable cause) {
    super(message, cause);
  }

  public CargoProofException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
