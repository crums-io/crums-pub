/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;


import io.crums.tc.notary.NotarySettings;

/**
 * 
 */
public class ServerSettings extends NotarySettings {
  
  
  public final static int DEFAULT_MAX_HASHES_PER_WITNESS = 8;
  
  
  private final int maxHashesPerWitness;
  

  public ServerSettings(NotarySettings notarySettings) {
    this(notarySettings, DEFAULT_MAX_HASHES_PER_WITNESS);
  }
  
  /**
   * 
   * @param notarySettings
   */
  public ServerSettings(
      NotarySettings notarySettings,
      int maxHashesPerWitness) {
    
    super(notarySettings);
    this.maxHashesPerWitness = maxHashesPerWitness;
    if (maxHashesPerWitness < 1)
      throw new IllegalArgumentException(
          "maxHashesPerWitness: " + maxHashesPerWitness);
  }


  
  public final int maxHashesPerWitness() {
    return maxHashesPerWitness;
  }

}
