/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;


import io.crums.tc.notary.CargoChain;

/**
 * A {@linkplain CargoChain#purgeInactiveBlocks()} run.
 * An instance can be run multiple times.
 */
public class PurgeRun extends Run {
  
  
  
  private volatile int cargoBlocksPurged;
  

  public PurgeRun(CargoChain cargoChain) {
    super(cargoChain);
  }
  

  @Override
  protected void runImpl() {
    cargoBlocksPurged = cargoChain.purgeInactiveBlocks();
  }
  


  /**
   * @return {@link #cargoBlocksPurged()} {@code > 0}
   */
  public boolean advanced() {
    return cargoBlocksPurged > 0;
  }
  

  /** Number of cargo blocks purged in the last run. */
  public int cargoBlocksPurged() {
    return cargoBlocksPurged;
  }

}
