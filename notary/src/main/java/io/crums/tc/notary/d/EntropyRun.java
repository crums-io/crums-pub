/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;

import java.security.SecureRandom;

import io.crums.tc.Constants;
import io.crums.tc.notary.CargoChain;
import io.crums.tc.notary.FreshCrum;

/**
 * 
 */
public class EntropyRun extends Run {

  private final SecureRandom random;
  
  
  private boolean finished;

  protected EntropyRun(CargoChain cargoChain) {
    super(cargoChain);
    this.random = new SecureRandom();
    // seed the random generator..
    // (seeds itself on first use)
    makeNoise();
  }
  
  
  private byte[] makeNoise() {
    byte[] noise = new byte[Constants.HASH_WIDTH];
    random.nextBytes(noise);
    return noise;
  }

  @Override
  protected void runImpl() {
    finished = false;
    var crum = new FreshCrum(makeNoise());
    cargoChain.addCrum(crum);
    finished = true;
  }

  @Override
  public synchronized boolean advanced() {
    return finished;
  }
  


  /**
   * Since we want to ensure in <em>every</em> block,
   * twice every block.
   * 
   * @return 2.0
   */
  @Override
  public float blockFrequency() {
    return 2f;
  }

}

















