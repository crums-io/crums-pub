/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;

/**
 * Indicates the cargo hash recorded in the chain conflicts with
 * with the hash (of a crum, or of the root of a Merkle tree of crums).
 * May be thrown on construction of a {@linkplain io.crums.tc.Crumtrail}.
 */
@SuppressWarnings("serial")
public class CargoConflictException extends BlockException {

  public CargoConflictException() {
  }

  public CargoConflictException(String message) {
    super(message);
  }

  public CargoConflictException(Throwable cause) {
    super(cause);
  }

  public CargoConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  public CargoConflictException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
