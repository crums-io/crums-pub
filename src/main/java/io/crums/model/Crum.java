/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


import java.nio.ByteBuffer;


/**
 * A timestamp, witness to a hash. Instances are immutable. This is a 2-tuple: a hash, and a UTC time.
 * 
 * <h3>Version 1 Note</h3>
 * <p>
 * This used to be a 3-tuple in version 0. The third field was a rather beefy 256-byte signature derived
 * from the other 2 fields. The signature was meant to bridge the gap in time from when a hash is first witnessed
 * to minutes later when it makes it into the tree. In truth, until the tree is published, the signature doesn't <em>prove</em>
 * anything. It's only usecase is when a user wants to show a hash to another person before the tree is published
 * and that other person already trusts crums.io. That's a tall story for a usecase, especially given the unneeded bloat
 * and complexity it introduces, so the signature was altogether dropped. A signatured version can be layered on top,
 * if the need/usecase arises.
 * </p>
 * 
 * @see #compareTo(Crum) Notable Version 1 changes in Crum ordering
 */
public class Crum extends HashUtc implements Comparable<Crum> {
  
  
  public static Crum newSearchKey(byte[] hash) {
    return new Crum(hash, Crum.INCEPTION_UTC);
  }
  public static Crum newSearchKey(ByteBuffer hash) {
    return new Crum(hash, Crum.INCEPTION_UTC);
  }
  


  public Crum(ByteBuffer data) {
    super(data);
  }

  public Crum(byte[] hash, long utc) {
    super(hash, utc);
  }

  public Crum(ByteBuffer hash, long utc) {
    super(hash, utc);
  }

  /**
   * Instances are ordered firstly by hash; secondly in time. Consistent with equals.
   */
  @Override
  public int compareTo(Crum o) {
    int comp = HASH_COMPARATOR.compare(this, o);
    return comp == 0 ? Long.compare(utc(), o.utc()) : comp;
  }

}
