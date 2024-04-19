/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;

/**
 * Indicates a chain state proof does contain the necessary block
 * to complete a {@linkplain io.crums.tc.Crumtrail crum trail}. 
 */
@SuppressWarnings("serial")
public class BlockNotFoundException extends BlockException {

  public BlockNotFoundException() {
  }

  public BlockNotFoundException(String message) {
    super(message);
  }

  public BlockNotFoundException(Throwable cause) {
    super(cause);
  }

  public BlockNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public BlockNotFoundException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
