/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.except;

/**
 * Merkle proof failure.
 */
@SuppressWarnings("serial")
public class MerkleProofException extends CargoProofException {

  public MerkleProofException() {  }

  public MerkleProofException(String message) {
    super(message);
  }

  public MerkleProofException(Throwable cause) {
    super(cause);
  }

  public MerkleProofException(String message, Throwable cause) {
    super(message, cause);
  }

  public MerkleProofException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
