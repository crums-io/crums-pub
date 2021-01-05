/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


import static io.crums.model.Constants.HASH_WIDTH;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Date;

import io.crums.util.IntegralStrings;

/**
 * A 32 byte hash, plus an 8 byte <tt>long</tt> representing a witness time.
 */
public class HashUtc {
  
  /**
   * Compares 2 instances solely by {@linkplain #hash() hash}.
   */
  public final static Comparator<HashUtc> HASH_COMPARATOR = new Comparator<HashUtc>() {
    @Override
    public int compare(HashUtc a, HashUtc b) {
      for (int index = 0; index < HASH_WIDTH; ++index) {
        int comp = Byte.compare(a.data.get(index), b.data.get(index));
        if (comp != 0)
          return comp;
      }
      return 0;
    }
  };
  
  /**
   * Version 1 concept date. We're ditching our existing data, so this'll be okay.
   * A claim to a hash witnessed in the form of a crum before this date (July 1, 2020 12:00:00 PM GMT) is
   * nonsense. The constructor disallows it.
   * 
   * @see #RUN_MAX_UTC
   */
  public final static long INCEPTION_UTC = 1593604800000L;
  
  /**
   * As a sanity check on data, the maximum UTC is bounded by 1 year from class-load time. So this means
   * you can't leave a server running forever. Not unrealistic.
   * 
   * @see #INCEPTION_UTC
   */
  public final static long RUN_MAX_UTC = System.currentTimeMillis() + 31_536_000_000L;
  
  /**
   * Data size is {@linkplain Constants#HASH_WIDTH} + {@linkplain Long#BYTES}.
   */
  public final static int DATA_SIZE = HASH_WIDTH + 8;
  
  
  
  /**
   * Serializes the given hash/utc tuple to the given <tt>out</tt> buffer. The point of this method
   * is to centralize serialization/deserializaton format (since it's changed).
   * 
   * @param hash {@linkplain Constants#HASH_WIDTH} bytes long
   * @param utc (not bounds checked)
   * @param out assumed to have remaining bytes. On return, assuming good
   *  arguments, the position is advanced {@linkplain #DATA_SIZE} bytes.
   *  
   *  @return the <tt>out</tt> argument (unflipped)
   * 
   * @see #HashUtc(ByteBuffer) constructor for <em>deserialization</em>.
   * @see #writeToBuffer(ByteBuffer, long, ByteBuffer)
   */
  public static ByteBuffer writeToBuffer(byte[] hash, long utc, ByteBuffer out) {
    if (hash.length != Constants.HASH_WIDTH)
      throw new IllegalArgumentException("hash.length: " + hash.length);
    out.put(hash).putLong(utc);
    return out;
  }
  

  /**
   * Serializes the given hash/utc tuple to the given <tt>out</tt> buffer. The point of this method
   * is to centralize serialization/deserializaton format (since it's changed).
   * 
   * @param hash {@linkplain Constants#HASH_WIDTH} remaning bytes
   * @param utc (not bounds checked)
   * @param out assumed to have remaining bytes. On return, assuming good
   *  arguments, the position is advanced {@linkplain #DATA_SIZE} bytes.
   *  
   *  @return the <tt>out</tt> argument (unflipped)
   * 
   * @see #HashUtc(ByteBuffer) constructor for <em>deserialization</em>.
   * @see #writeToBuffer(byte[], long, ByteBuffer)
   */
  public static ByteBuffer writeToBuffer(ByteBuffer hash, long utc, ByteBuffer out) {
    if (hash.remaining() != Constants.HASH_WIDTH)
      throw new IllegalArgumentException("hash.remaining(): " + hash.remaining());
    
    if (out.remaining() < hash.remaining())
      throw new BufferOverflowException();
    out.put(hash.duplicate()).putLong(utc);
    return out;
  }
  
  
  
  

  /**
   * The positional state is <em>never</em> modified, not even momentarily.
   * (If a subclass does, then it'll likely have undefined concurrent behavior.)
   */
  protected final ByteBuffer data;
  


  /**
   * Creates a new instance with the given data buffer.
   * 
   * @param data  the caller agrees not to modify the given buffer in any way; otherwise the immutability
   *    the immutability guarantee is for naught.
   *    
   * @see #serialForm()
   */
  public HashUtc(ByteBuffer data) {
    
    if (data.remaining() < DATA_SIZE)
      throw new IllegalArgumentException("data remaining: " + data.remaining());
    
    this.data = data.position() == 0 ? data : data.slice();
    // a quick but effectve sanity check..
    sanityCheckUtc(utc());
  }
  
  
  /**
   * Creates a new instance with the given fields.
   * 
   * @see #INCEPTION_UTC
   * @see #RUN_MAX_UTC
   */
  public HashUtc(byte[] hash, long utc) {
    
    sanityCheckUtc(utc);
    
    this.data = writeToBuffer(hash, utc, ByteBuffer.allocate(DATA_SIZE)).flip();
  }
  
  
  /**
   * Creates a new instance with the given fields.
   * 
   * @see #INCEPTION_UTC
   * @see #RUN_MAX_UTC
   */
  public HashUtc(ByteBuffer hash, long utc) {

    sanityCheckUtc(utc);
    
    this.data = writeToBuffer(hash, utc, ByteBuffer.allocate(DATA_SIZE)).flip();
  }

  
  /**
   * Bounds checks the given <tt>utc</tt> as one that could possibly be issued by the service.
   */
  protected final void sanityCheckUtc(long utc) {
    if (utc < INCEPTION_UTC)
      throw new IllegalArgumentException(
          "utc " + utc + " (" + new Date(utc) + ") < project inception utc " + INCEPTION_UTC);
    if (utc > RUN_MAX_UTC)
      throw new IllegalArgumentException(
          "utc " + utc + " (" + new Date(utc) + ") >  RUN_MAX_UTC (year + server start time) " + RUN_MAX_UTC);
  }
  
  
  
  /**
   * Returns a read-only buffer whose remaining bytes are the hash. (It's capacity is
   * in fact greater.)
   */
  public ByteBuffer hash() {
    return data.asReadOnlyBuffer().limit(HASH_WIDTH);
  }
  
  /**
   * Returns the {@linkplain #hash() hash} in its hexadecimal representation.
   */
  public String hashHex() {
    return IntegralStrings.toHex(hash());
  }
  
  
  /**
   * Time in UTC milliseconds.
   */
  public long utc() {
    return data.getLong(HASH_WIDTH);
  }
  

  /**
   * Returns the instance's serialized representation as a read-only buffer.
   * 
   * @return a buffer with {@linkplain #DATA_SIZE} remaining bytes.
   * 
   * @see #Crum(ByteBuffer)
   */
  public ByteBuffer serialForm() {
    return data.asReadOnlyBuffer().limit(DATA_SIZE);
  }
  
  /**
   * Instances are equal iff their {@linkplain #hash()}s and {@linkplain #utc()}s  are equal.
   * 
   * @see #equalsHashUtc(HashUtc)
   */
  @Override
  public final boolean equals(Object o) {
    return o == this ||  o instanceof HashUtc && equalsHashUtc((HashUtc) o);
  }
  
  /**
   * Typed {@linkplain #equals(Object)}.
   */
  public final boolean equalsHashUtc(HashUtc o) {
    return HASH_COMPARATOR.compare(this, o) == 0 && utc() == o.utc();
  }
  
  /**
   * Tests if this instance's hash is the same as the other.
   */
  public final boolean hashEquals(HashUtc o) {
    return HASH_COMPARATOR.compare(this, o) == 0;
  }
  
  
  /**
   * Consistent with equals. And efficient. (We lean on the randomness of cryptographic hashes).
   */
  @Override
  public final int hashCode() {
    return data.getInt(0);
  }
  
  /**
   * For debug purposes.
   */
  public String toString() {
    StringBuilder string = new StringBuilder();
    string.append(getClass().getSimpleName()).append("[");
    string.append(IntegralStrings.toHex(hash().limit(8))).append("..+").append(utc()).append("]");
    return string.toString();
  }

}
