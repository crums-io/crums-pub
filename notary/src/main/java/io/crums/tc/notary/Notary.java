/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import static io.crums.tc.notary.NotaryConstants.CARGO_DIR;
import static io.crums.tc.notary.NotaryConstants.CHAIN;
import static io.crums.tc.notary.NotaryConstants.NOTARY_PROPS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import io.crums.io.FileUtils;
import io.crums.tc.ChainParams;
import io.crums.tc.Crum;
import io.crums.tc.TimeBinner;
import io.crums.tc.TimeChain;
import io.crums.util.TaskStack;

/**
 * 
 */
public class Notary implements Channel {
  
  
  /**
   * Incepts a new notary in the given directory and returns it.
   * 
   * 
   * @param dir               directory notary will live in (created on demand)
   * @param binner            determimnes the time interval each block spans; 
   *                          &ge; {@linkplain TimeBinner#QUARTER_SEC}
   * @param startUtc          inception UTC (truncated to time bin boundary)
   * @param blocksRetained    the number of rolling cargo blocks
   *                          (&ge; 3, but allow more.. way more);
   *                          see {@link NotaryPolicy#blocksRetained()}
   */
  public static Notary incept(
      File dir,
      TimeBinner binner,
      long startUtc,
      int blocksRetained)
  
          throws IOException {
    
    
    var settings = new NotarySettings(
        ChainParams.forStartUtc(binner, startUtc),
        blocksRetained);
    
    return incept(dir, settings);
    
  }
  
  
  
  
  /**
   * Incepts a new notary in the given directory and returns it. Creates
   * a time-chain file and a "CARGO" subdirectory.
   * 
   * @param dir       path to directory notary will live in
   * @param settings  (includes chain params)
   */
  public static Notary incept(File dir, NotarySettings settings)
      throws IOException {
    
    FileUtils.ensureDir(dir);
    
    final File chainFile = new File(dir, CHAIN);
    final File settingsFile = new File(dir, NOTARY_PROPS);
    
    try (var closeOnFail = new TaskStack()) {
      
      // write the settings file
      {
        var writer = new FileOutputStream(settingsFile);
        closeOnFail.pushClose(writer);
        
        var comment = "Notary settings";
        settings.toProperties().store(writer, comment);
        writer.close();
      }
      
      TimeChain chain =
          TimeChain.inceptNewChain(
              chainFile,
              settings.chainParams().timeBinner(),
              settings.chainParams().inceptionUtc());
      
      closeOnFail.pushClose(chain);
      
      var notary = new Notary(chain, settings);
      
      closeOnFail.clear();
      
      return notary;
    }
  }
  
  
  
  
  /**
   * Loads and returns an instance from the given directory.
   */
  public static Notary load(File dir) throws IOException {
    
    final File chainFile = new File(dir, CHAIN);
    final File settingsFile = new File(dir, NOTARY_PROPS);
    
    
    
    try (var closeOnFail = new TaskStack()) {
      
      var chain = TimeChain.load(chainFile);
      closeOnFail.pushClose(chain);
      
      var settings = NotarySettings.load(settingsFile, chain.params());
      
      var notary = new Notary(chain, settings);
      closeOnFail.clear();
      
      return notary;
    }
    
  }
  
  
  
  
  
  
  
  
  protected final CargoChain cargoChain;
  
  
  
  /**
   * {@code this(chain, settings, null, NotaryLog.SYS)}.
   * @see #Notary(TimeChain, NotarySettings, File, NotaryLog)
   */
  protected Notary(
      TimeChain chain, NotarySettings settings) {
    this(chain, settings, null, NotaryLog.SYS);
  }
  
  
  /**
   * Full-parameter, main constructor.
   * 
   * @param chain         the time chain
   * @param settings      its chain-params must match that of {@code chain}
   * @param cargoChainDir if {@code null} then the default diretory named 
   *                      {@linkplain NotaryConstants#CARGO_DIR CARGO is
   *                      chosen
   * @param log           not {@code null}
   */
  protected Notary(
      TimeChain chain, NotarySettings settings,
      File cargoChainDir,
      NotaryLog log) {
    
    cargoChainDir = cargoChainDir(cargoChainDir, chain);
    var bccArgs =
        new CargoChain.InitArgs(chain, settings, cargoChainDir, log);
    
    this.cargoChain = new CargoChain(bccArgs);
  }
    
  
  
  private File cargoChainDir(File cargoChainDir, TimeChain chain) {
    
    return
        cargoChainDir == null ?
            new File(
                chain.file().getParentFile(),
                CARGO_DIR)
            :
                  
            cargoChainDir;
  }
  
  
  
  
  
  
  
  
  
  
  @Override
  public boolean isOpen() {
    return cargoChain.isOpen();
  }


  @Override
  public void close() {
    cargoChain.close();
  }
  
  
  
  protected TimeChain timechain() {
    return cargoChain.timechain();
  }
  
  /**
   * Returns the number of blocks committed to the time chain.
   * 
   * @return
   */
  public long blockCount() {
    return timechain().size();
  }
  
  
  public ChainParams chainParams() {
    return cargoChain.timechain().params();
  }
  
  
  
  public NotarySettings settings() {
    return cargoChain.settings();
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







