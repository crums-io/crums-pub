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
 * power of 2.</li>
 * <li>The starting time of the first block.</li>
 * </ol>
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
  
  
  /**
   * <p>Instances are equal if they have the same time binner and
   * inception UTC.</p>
   * {@inheritDoc}
   */
  @Override
  public final boolean equals(Object o) {
    return o == this ||
        o instanceof ChainParams other &&
        inceptionUtc == other.inceptionUtc &&
        binInterval == other.binInterval;
  }
  
  
  /**
   * <p>Consistent with {@linkplain #equals(Object)}.</p>
   * {@inheritDoc}
   */
  @Override
  public final int hashCode() {
    return Long.hashCode(inceptionUtc) ^ (int) binInterval;
  }
  
  
  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "[" + inceptionUtc + ":" + timeBinner.binExponent() + "]";
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
   * Checks and returns the block number the given {@code utc} falls in.
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
   * Returns the block number the given {@code utc} falls in. Only
   * positive return values are valid.
   */
  public final long blockNoForUtcUnchecked(long utc) {
    return timeBinner.toBinNo(utc - inceptionUtc) + 1;
  }
  
  
  
  /**
   * Determines whether the given {@code utc} falls in the given
   * {@code blockNo}.
   */
  public final boolean utcInBlock(long utc, long blockNo) {
    return
        utc >= inceptionUtc &&
        timeBinner.toBinNo(utc - inceptionUtc) + 1 == blockNo;
    
  }
  
  /**
   * Returns the starting (smallest) UTC value for the given
   * block no.
   */
  public final long utcForBlockNo(long block) {
    return inceptionUtc + (block - 1) * binInterval;
  }
  



  public final long blockDuration() {
    return timeBinner.duration();
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
