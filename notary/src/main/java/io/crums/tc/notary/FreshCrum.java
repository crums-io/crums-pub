/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import java.nio.ByteBuffer;

import io.crums.tc.Crum;

/**
 * A fresh {@linkplain Crum}: its {@linkplain Crum#utc() UTC}
 * value is the system time at construction. The point of
 * defining this type is not convenience: the type is useful
 * on input because a reference to one comes with a degree of
 * guarantee about its UTC value:
 * <ol>
 * <li>Its UTC value is never greater than the system time.</li>
 * <li>Depending on context (how long an instance is kept in memory),
 * its UTC value cannot be very far the system time.</li>
 * </ol>
 */
public class FreshCrum extends Crum {

  /** Constructs an instance with the current system time. */
  public FreshCrum(ByteBuffer hash) {
    super(hash, System.currentTimeMillis());
  }

  /** No-copy UTC buffer. */
  public final ByteBuffer utcBuffer() {
    return serialize().position(DATA_SIZE - 8);
  }

}
