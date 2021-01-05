/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


import java.util.Objects;

/**
 * A "trailed" <tt>CrumRecord</tt>.
 */
public final class TrailedRecord extends CrumRecord {
  
  private final CrumTrail trail;

  /**
   * Creates an instance with the given crum <tt>trail</tt>.
   */
  public TrailedRecord(CrumTrail trail) {
    this.trail = Objects.requireNonNull(trail, "null trail");
  }

  /**
   * @return <tt>true</tt>
   */
  @Override
  public boolean isTrailed() {
    return true;
  }

  @Override
  public Crum crum() {
    return trail.crum();
  }

  /**
   * @return non-<tt>null</tt>
   */
  @Override
  public CrumTrail trail() {
    return trail;
  }

}
