/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import static io.crums.tc.notary.NotaryConstants.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import io.crums.io.FileUtils;
import io.crums.io.Opening;
import io.crums.tc.Crum;
import io.crums.tc.Crumtrail;
import io.crums.tc.TimeChain;

/**
 * 
 */
public class Notary {
  
  
  
  
  private final CargoChain cargoChain;
  
//  private final NotaryLog log;

  
  
  

  public Notary(
      TimeChain chain, NotarySettings settings, NotaryLog log) {
    this(chain, settings, null, log);
  }
  
  
  /**
   * 
   */
  public Notary(
      TimeChain chain, NotarySettings settings,
      File cargoChainDir,
      NotaryLog log) {
    
    cargoChainDir = cargoChainDir(cargoChainDir, chain);
    var bccArgs =
        new CargoChain.InitArgs(chain, settings, cargoChainDir, log);
    
    this.cargoChain = new CargoChain(bccArgs);
//    this.log = log;
  }
    
  
  
  private File cargoChainDir(File cargoChainDir, TimeChain chain) {
    if (cargoChainDir == null) {
      File chainDir = FileUtils.getParentDir(chain.file());
      cargoChainDir = new File(chainDir, CARGO_CHAIN_DIR);
    }
    return cargoChainDir;
  }
  
  
  
  public Receipt witness(ByteBuffer hash) {
    return cargoChain.findReceipt(hash).orElseGet(
        () -> cargoChain.addCrum(new FreshCrum(hash)));
  }
  
  
  public Receipt update(Crum crum) {
    return cargoChain.findReceipt(crum).orElseGet(
        () -> cargoChain.addCrum(new FreshCrum(crum.hash())));
  }

  
  
  

}







