/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;

import java.util.Optional;

/**
 * 
 */
@SuppressWarnings("serial")
public class BlockException extends TimeChainException {
  
  private long blockNo;

  public BlockException() {
  }
  
  
  public BlockException setBlockNo(long blockNo) {
    this.blockNo = blockNo;
    return this;
  }
  
  public Optional<Long> blockNo() {
    return blockNo <= 0 ? Optional.empty() : Optional.of(blockNo);
  }
  

  public BlockException(String message) {
    super(message);
  }

  public BlockException(Throwable cause) {
    super(cause);
  }

  public BlockException(String message, Throwable cause) {
    super(message, cause);
  }

  public BlockException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
