/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;

import java.util.Objects;

/**
 * A <tt>CrumRecord </tt> in the "untrailed" state.
 */
public class UntrailedRecord extends CrumRecord {

  private final Crum crum;
  
  /**
   * Creates an instance using the given <tt>crum</tt>.
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
   * @return <tt>false</tt>
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
   * @return <tt>null</tt>
   */
  @Override
  public final CrumTrail trail() {
    return null;
  }

}
