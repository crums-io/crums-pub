/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;

import java.util.Objects;

/**
 * A <tt>CrumRecord </tt> in the "untrailed" state.
 */
public final class UntrailedRecord extends CrumRecord {

  private final Crum crum;
  
  /**
   * Creates an instance using the given <tt>crum</tt>.
   */
  public UntrailedRecord(Crum crum) {
    this.crum = Objects.requireNonNull(crum, "null crum");
  }

  /**
   * @return <tt>false</tt>
   */
  @Override
  public boolean isTrailed() {
    return false;
  }

  
  @Override
  public Crum crum() {
    return crum;
  }

  /**
   * @return <tt>null</tt>
   */
  @Override
  public CrumTrail trail() {
    return null;
  }

}
