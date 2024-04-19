/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


import java.util.Objects;

/**
 * A "trailed" <code>CrumRecord</code>.
 */
public final class TrailedRecord extends CrumRecord {
  
  private final CrumTrail trail;

  /**
   * Creates an instance with the given crum <code>trail</code>.
   */
  public TrailedRecord(CrumTrail trail) {
    this.trail = Objects.requireNonNull(trail, "null trail");
  }

  /**
   * @return <code>true</code>
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
   * @return non-<code>null</code>
   */
  @Override
  public CrumTrail trail() {
    return trail;
  }

}
