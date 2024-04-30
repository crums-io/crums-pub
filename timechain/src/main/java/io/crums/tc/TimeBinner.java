/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

/**
 * Time bins defined over powers of 2, in milliseconds.
 * Note the names used here often
 * connote only a <em>ballpark</em> idea about their durations.
 * The coarsest resolution is roughly a day long (less some 5hrs 20min).
 * Expected real-world use is for bin spanning seconds and above;
 * the more fine grained resolutions are mostly used in tests.
 * 
 * <h2>Motivation</h2>
 * <p>
 * Listing some motivating factors behind this..
 * </p>
 * <ol>
 * <li>Universal time bin boundaries. The boundaries are
 * globally pre-defined. We don't have to negotiate
 * inception times this way.</li>
 * <li>Easier to merge. Smaller bins fit neatly into
 * larger bins; since their boundaries are pre-determined,
 * they're easier to merge.</li>
 * <li>Easier arithmetic. Division is costly; division by exact
 * powers of 2, not so much. (This aspect is really a minor
 * convenience.)</li>
 * </ol>
 * <h2>TODO</h2>
 * <p>
 * Document the larger bins in minutes and seconds.
 * </p>
 */
public enum TimeBinner {
  
  /** 2 millis. */
  MILLIS_2(1),
  /** 4 millis. */
  MILLIS_4(2),
  /** 8 millis. */
  MILLIS_8(3),
  /** 16 millis. */
  MILLIS_16(4),
  /** 32 millis. */
  MILLIS_32(5),
  /** 64 millis. */
  MILLIS_64(6),
  /** 128 millis. */
  EIGTH_SEC(7),
  /** 256 millis. */
  QUARTER_SEC(8),
  /** 512 millis, or half a second and 12 millis. */
  HALF_SEC(9),
  /** 1024 millis, or a second and 24 millis. */
  SEC(10),
  /** 2048 millis, roughly 2.05 seconds. */
  SEC_2(11),
  /** 4096 millis, about 4.1 seconds. */
  SEC_4(12),
  /** 8192 millis, about 8.2 seconds. */
  SEC_8(13),
  /** 16384 millis, about 16.4 seconds. */
  SEC_16(14),
  /** 0m:33s */
  HALF_MINUTE(15),
  /** 1m:06s */
  MINUTE(16),
  /** 2m:11s */
  MINUTE_2(17),
  /** 4m:56s */
  MINUTE_5(18),
  /** 8m:44s */
  MINUTE_9(19),
  /** 17m:29s */
  MINUTE_17(20),
  /** 34m:57s */
  HALF_HOUR(21),
  /** 1h:10m */
  HOUR(22),
  /** 2h:20m */
  HOUR_2(23),
  /** 4h:40m */
  HOUR_5(24),
  /** 9h:19m  */
  HOUR_9(25),
  /** 18h:38m */
  HOUR_19(26);
  // Psst.. on adding more elements, update comment for MAX_EXP
  
  
  /** Minimum bin exponent (1). Inclusive. */
  public final static int MIN_EXP = 1;
  /** Maximum bin exponent (26). Inclusive. */
  public final static int MAX_EXP = values().length;
  
  
  /**
   * The bit mask is partitioned into 2 contiguous regions:(leading) 1 bits on the left and (trailing) 0 bits on the right.
   */
  public final long mask;
  
  /**
   * The number of [leading] zeroes in the {@linkplain #mask}.
   * This is the value returned by {@linkplain #binExponent()}.
   * 1 &le; {@code shift} &le; {@link #MAX_EXP}
   */
  public final int shift;
  
  
  private TimeBinner(int shift) {
    this.mask = (-1L << shift);
    this.shift = shift;
  }
  
  /**
   * Returns the bin's duration of a bin in milliseconds.
   * 
   * @return {@code 1 << binExponent()}
   */
  public int duration() {
    return 1 << shift;
  }
  
  
  /**
   * Returns the power of 2 that defines the units and duration of bins.
   * The smaller the value, the greater the bin resolution.
   * 
   * @return positive and &le; 16
   */
  public int binExponent() {
    return shift;
  }
  
  
  /**
   * Converts the given UTC to a bin no.
   * 
   * @param utc positive epoch millis
   * @return {@linkplain #binTime(long) binTime(utc)} &gt;&gt; {@linkplain #shift}
   */
  public long toBinNo(long utc) {
    return (utc & mask) >> shift;
  }
  
  
  
  /**
   * Returns the "representative" bin time for the given UTC.
   * Here a bin is represented by the smallest UTC value
   * that belongs in it.
   * 
   * @param utc positive epoch millis
   * 
   * @return {@code utc & MASK} (smallest UTC value that maps to the same bin
   *         that {@code utc} falls in)
   */
  public long binTime(long utc) {
    return utc & mask;
  }
  
  

  /**
   * Converts time as expressed in units of this bin to UTC.
   * 
   * @param binNo bin no (see {@linkplain #toBinNo(long)}
   * 
   * @return {@code binNo << shift}
   */
  public long toUtc(long binNo) {
    return binNo << shift;
  }

  
  /**
   * Returns the binner by {@linkplain #binExponent() bin exponent}.
   */
  public static TimeBinner forExponent(int exponent) {
    if (exponent < MIN_EXP)
      throw new IllegalArgumentException("exponent too small: " + exponent);
    if (exponent > MAX_EXP)
      throw new IllegalArgumentException("exponent too large: " + exponent);
    
    TimeBinner[] bins = TimeBinner.values();
    return bins[exponent - 1];
  }


}
