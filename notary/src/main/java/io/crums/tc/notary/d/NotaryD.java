/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.crums.tc.notary.Notary;
import io.crums.util.TaskStack;

/**
 * Notary with background commit-, purge-, and entropy- daemons.
 * 
 * @see #close()
 */
public class NotaryD extends Notary {
  
  

  private final ExecutorService executor;
  
  private final Daemon<CommitRun> commitD;
  private final Daemon<PurgeRun> purgeD;
  
  /** Optional: may be null. */
  private final Daemon<EntropyRun> noiseD;
  
  
  /**
   * Creates an instance, with the entropy daemon running.
   * 
   * @param promote   the "basic" instance
   */
  public NotaryD(Notary promote) {
    this(promote, true);
  }
  
  
  /**
   * Creates an instance with using a fixed (regular) thread pool.
   * 
   * @param promote   the "basic" instance
   * @param noise     if {@code false}, then the entropy daemon is not run
   */
  public NotaryD(Notary promote, boolean noise) {
    this(promote, Executors.newFixedThreadPool(noise ? 3 : 2), noise);
  }
  
  
  /**
   * Full constructor. Promotes the given "basic" instance.
   * 
   * @param promote   the "basic" instance
   * @param executor  thread pool (virtual or regular),
   *                  owned by the instance.
   * @param noise     if {@code false}, then the entropy daemon is not run
   */
  public NotaryD(Notary promote, ExecutorService executor, boolean noise) {
    super(promote);
    if (promote instanceof NotaryD)
      throw new IllegalArgumentException(
          "promote is an instance of NotaryD: " + promote);
    this.executor = executor;
    this.commitD = newCommitDaemon();
    this.purgeD = newPurgeDaemon();
    
    if (noise) {
      this.noiseD = newEntropyDaemon();
      executor.execute(noiseD);
    } else {
      this.noiseD = null;
    }
    executor.execute(commitD);

    this.cargoChain.sweepGraveyard();
    executor.execute(purgeD);
  }



  
  
  protected Daemon<CommitRun> newCommitDaemon() {
    return new Daemon<>(new CommitRun(cargoChain));
  }
  
  
  protected Daemon<PurgeRun> newPurgeDaemon() {
    return new Daemon<>(new PurgeRun(cargoChain));
  }
  
  
  protected Daemon<EntropyRun> newEntropyDaemon() {
    return new Daemon<>(new EntropyRun(cargoChain));
  }

  
  
  /**
   * Shuts down the background daemons, before closing
   * the underlying instance (which boils down to closing
   * its open timechain file).
   */
  @Override
  public void close() {
    try (var closer = new TaskStack()) {
      closer.pushClose(cargoChain, commitD, purgeD);
      if (noiseD != null) {
        closer.pushClose(noiseD);
        noiseD.stop();
      }
      commitD.stop();
      purgeD.stop();
      executor.shutdown();
    }
  }

  

}
















