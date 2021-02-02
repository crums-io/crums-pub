/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;

import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.StringTokenizer;

import io.crums.util.IntegralStrings;

/**
 * An immutable, non-empty sequence of bytes with flexible (whitespace delimitable)
 * hex representation. Note this class was designed before the parser was completed;
 * it contains unused functionality that is best deleted.
 */
final class ByteString extends BaseEntity {
  
  
  private final ByteBuffer bytes;
  
  private final String hex;
  
  public ByteString(ByteBuffer remaining) {
    if (!Objects.requireNonNull(remaining, "null remaining").hasRemaining())
      throw new IllegalArgumentException("buffer arg has no remaining bytes");
    this.bytes =
        ByteBuffer.allocate(remaining.remaining())
        .put(remaining).flip().asReadOnlyBuffer();
    
    if (!bytes.hasRemaining())
      throw new ConcurrentModificationException("on input argument " + remaining);
    
    this.hex = IntegralStrings.toHex(bytes);
  }
  
  
  public ByteString(String wsdHex) {
    Objects.requireNonNull(wsdHex, "null wsdHex");
    StringTokenizer hexTokens = new StringTokenizer(wsdHex);
    if (!hexTokens.hasMoreTokens())
      throw new IllegalArgumentException("empty wsdHex: '" + wsdHex + "'");
    
    CharSequence hexes;
    String first = hexTokens.nextToken();
    if (hexTokens.hasMoreTokens()) {
      
      StringBuilder hx = new StringBuilder(first);
      while (hexTokens.hasMoreTokens()) {
        
        String hex = hexTokens.nextToken();
        if (!IntegralStrings.isHex(hex))
          throw new IllegalArgumentException("'" + hex + "' in '" + wsdHex + "'");
        
        hx.append(hex);
      }
      hexes = hx;
      
    } else
      hexes = first;
    
    byte[] b = IntegralStrings.hexToBytes(hexes);
    this.bytes = ByteBuffer.wrap(b).asReadOnlyBuffer();
    this.hex = wsdHex;
  }
  
  
  private ByteString(ByteBuffer bytes, String hex) {
    this.bytes = bytes;
    this.hex = hex;
  }
  
  /**
   * Returns a read-only view of the bytes.
   */
  public ByteBuffer bytes() {
    return bytes.asReadOnlyBuffer();
  }
  
  /**
   * Returns the number of bytes in the sequence.
   */
  public int length() {
    return bytes.remaining();
  }
  
  /**
   * Returns the hexadecimal representation of the sequence. <em>May
   * be whitespace delimited</em>: if so, every contiguous hex sequence has even
   * length (i.e. a whole number bytes).
   * 
   * @see #bytesHex()
   */
  public String hexSequence() {
    return hex;
  }
  
  
  public ByteString append(ByteString other) {
    Objects.requireNonNull(other, "ByteString.append(null)");
    
    ByteBuffer bOut =
        ByteBuffer.allocate(length() + other.length())
        .put(bytes()).put(other.bytes()).flip().asReadOnlyBuffer();
    
    String hexOut = hexSequence() + ' ' + other.hexSequence();
    
    return new ByteString(bOut, hexOut);
  }
  
  
  
  
  /**
   * Returns {@linkplain #hexSequence()}.
   */
  @Override
  public String toText() {
    return hexSequence();
  }


  @Override
  public boolean delimited() {
    return bytes.remaining() * 2 == hex.trim().length();
  }
  
  
  public boolean isComplete() {
    return true;
  }

}











