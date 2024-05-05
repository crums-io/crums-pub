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
 */
public class NotaryD extends Notary {
  
  

  private final ExecutorService executor;
  
  private final Daemon<CommitRun> commitD;
  private final Daemon<PurgeRun> purgeD;
  /** Optional: may be null. */
  private final Daemon<EntropyRun> noiseD;
  
  
  
  public NotaryD(Notary promote) {
    this(promote, Executors.newFixedThreadPool(3), false);
  }
  
  
  /**
   * Constructs an instance using the given
   * "basic" instance.
   * 
   * @param promote   the "basic" instance
   * @param executor  thread pool (virtual or o.w.),
   *                  owned by the instance.
   * @param noNoise   if {@code false}, then the entropy daemon is not run
   */
  public NotaryD(Notary promote, ExecutorService executor, boolean noNoise) {
    super(promote);
    if (promote instanceof NotaryD)
      throw new IllegalArgumentException(
          "promote is an instance of NotaryD: " + promote);
    this.executor = executor;
    this.commitD = newCommitDaemon();
    this.purgeD = newPurgeDaemon();
    
    if (noNoise) {
      this.noiseD = null;
    } else {
      this.noiseD = newEntropyDaemon();
      executor.execute(noiseD);
    }
    executor.execute(commitD);
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
















