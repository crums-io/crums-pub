/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

import io.crums.io.Serial;

/**
 * Chain parameter settings. Presently contains only 2 pieces of
 * information:
 * 
 * <ol>
 * <li>No. of milliseconds a block spans. This is always a positive
 * power of 2 in order to simplify the arithmetic.</li>
 * <li>The starting time of the first block.</li>
 * </ol>
 * 
 */
public class ChainParams implements Serial {
  
  
  
  
  /**
   * Creates and returns an instance. Less finicky than constructor.
   * 
   * @return {@code new ChainParams(timeBinner, timeBinner.binTime(utcStart))}
   */
  public static ChainParams forStartUtc(TimeBinner timeBinner, long utcStart) {
    return new ChainParams(timeBinner, timeBinner.binTime(utcStart));
  }
  
  
  public final static int BYTE_SIZE = 1 + 8;
  
  private final TimeBinner timeBinner;
  private final long inceptionUtc;
  private final long binInterval;
  

  /**
   * Creates a new instance. The {@code inceptionUtc} must lie at the
   * beginning at time bin boundary defined by the {@code timeBinner}.
   * I.e. {@code inceptionUtc == timeBinner.binTime(inceptionUtc)}
   * must be {@code true}.
   * 
   * @see #forStartUtc(TimeBinner, long)
   */
  public ChainParams(TimeBinner timeBinner, long inceptionUtc) {
    this.timeBinner = Objects.requireNonNull(timeBinner, "null timeBinner");
    this.inceptionUtc = inceptionUtc;
    
    if (inceptionUtc < HashUtc.INCEPTION_UTC)
      throw new IllegalArgumentException(
          "inception UTC impossibly low: " + inceptionUtc);
    if (inceptionUtc > System.currentTimeMillis())
      throw new IllegalArgumentException(
          "inception UTC ahead of system time: " + inceptionUtc);
    if (inceptionUtc != timeBinner.binTime(inceptionUtc))
      throw new IllegalArgumentException(
          "inception UTC does not aligned at time bin boundary: " +
          inceptionUtc);
    
    this.binInterval = timeBinner.duration();
  }
  
  
  
  public final TimeBinner timeBinner() {
    return timeBinner;
  }
  
  /**
   * Returns the inception UTC. Equivalently, this is
   * the UTC for the first block in the chain.
   */
  public final long inceptionUtc() {
    return inceptionUtc;
  }
  
  
  /**
   * Returns the block number the given {@code utc} falls in.
   * 
   * @throws IllegalArgumentException
   *         if {@code utc < inceptionUtc()}
   */
  public final long blockNoForUtc(long utc)
      throws IllegalArgumentException {
    if (utc < inceptionUtc)
      throw new IllegalArgumentException(
          "utc (" + utc + ") < inception UTC (" + inceptionUtc + ")");
    return timeBinner.toBinNo(utc - inceptionUtc) + 1;
  }
  
  /**
   * Returns the starting (smallest) UTC value for the given
   * block no.
   */
  public final long utcForBlockNo(long block) {
    return inceptionUtc + (block - 1) * binInterval;
  }
  
  
  

  @Override
  public int serialSize() {
    return BYTE_SIZE;
  }

  @Override
  public ByteBuffer writeTo(ByteBuffer out) throws BufferOverflowException {
    out.put((byte) timeBinner.shift);
    out.putLong(inceptionUtc);
    return out;
  }
  
  
  /**
   * Loads and returns an instance, reading the given buffer.
   * 
   * @param data  advanced by {@linkplain ChainParams#BYTE_SIZE} bytes
   *              on return
   * @return      deserialized instance
   */
  public static ChainParams load(ByteBuffer data) {
    if (data.remaining() < BYTE_SIZE)
      throw new IllegalArgumentException(data.toString());
    int binExponent = data.get();
    TimeBinner timeBinner = TimeBinner.forExponent(binExponent);
    long inceptionUtc = data.getLong();
    return new ChainParams(timeBinner, inceptionUtc);
  }

}
