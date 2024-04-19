/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;

import java.util.Objects;

/**
 * A <code>CrumRecord </code> in the "untrailed" state.
 */
public class UntrailedRecord extends CrumRecord {

  private final Crum crum;
  
  /**
   * Creates an instance using the given <code>crum</code>.
   */
  public UntrailedRecord(Crum crum) {
    this.crum = Objects.requireNonNull(crum, "null crum");
  }
  
  
  /**
   * Copy constructor.
   */
  protected UntrailedRecord(UntrailedRecord copy) {
    this.crum = Objects.requireNonNull(copy, "null copy instance").crum();
  }

  /**
   * @return <code>false</code>
   */
  @Override
  public final boolean isTrailed() {
    return false;
  }

  
  @Override
  public final Crum crum() {
    return crum;
  }

  /**
   * @return <code>null</code>
   */
  @Override
  public final CrumTrail trail() {
    return null;
  }

}
