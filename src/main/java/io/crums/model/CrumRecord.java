/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;

import java.nio.ByteBuffer;

/**
 * <p>A record of the life of a witnessed hash. A witnessed hash goes through 3 phases, 2 of which are
 * represented by instances of this class.
 * </p>
 * <ol>
 *   <li><b>Created</b>. A crum, that is just a hash/utc pair, enters the database.</li>
 *   <li><b>Trailed</b>. The crum enters a Merkle tree.</li>
 *   <li><b>Trimmed</b>. The crum and its associated Merkle are purged from the database. The only
 *   artifact of the crum remaining in the database is the <em>root</em> of the Merkle tree it was in
 *   (to which a valid instance of this class is forever provably linked to).</li>
 * </ol>
 * <p>
 * An instance of this class represents of the first 2 states of a crum.
 * </p>
 */
public abstract class CrumRecord {
  
  /**
   * Determines whether the record has a crum trail. A <code>CrumRecord</code> begins life without a trail.
   * 
   * @return <code>true</code> iff {@linkplain #trail()} doesn't return <code>null</code>.
   * 
   * @see #trail()
   */
  public abstract boolean isTrailed();
  
  /**
   * Returns the record's crum (hash/utc pair).
   * 
   * @return never <code>null</code>
   */
  public abstract Crum crum();
  
  /**
   * Returns the crum trail, or <code>null</code> if it doesn't have one. (A crum has a trail once it's made it into a Merkle tree.)
   * 
   * @return possibly <code>null</code>
   * @see #isTrailed()
   */
  public abstract CrumTrail trail();
  
  
  
  /**
   * Instances are equal if their crums are equal and are both trailed.
   * 
   * @see #hashCode()
   */
  @Override
  public final boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof CrumRecord))
      return false;
    
    CrumRecord other = (CrumRecord) o;
    boolean equ = (isTrailed() == other.isTrailed()) && crum().equals(other.crum());
    
//  boolean insanity = equ && isTrailed() && !trail().equals(other.trail());
    assert !(equ && isTrailed() && !trail().equals(other.trail()));
    
    return equ;
  }
  
  
  /**
   * Consistent with {@linkplain #equals(Object)}.
   */
  @Override
  public final int hashCode() {
    return crum().hashCode();
  }
  

  /**
   * Returns the time witnessed.
   * 
   * @return {@code crum().utc()}
   */
  public final long utc() {
    return crum().utc();
  }
  
  
  /**
   * Returns the hash witnessed.
   * 
   * @return {@code crum().hash()}
   */
  public final ByteBuffer hash() {
    return crum().hash();
  }
  

  /**
   * Returns the {@linkplain #hash() hash} in hexadecimal representation.
   */
  public final String hashHex() {
    return crum().hashHex();
  }

}










