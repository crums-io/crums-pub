/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;


import io.crums.tc.notary.CargoChain;

/**
 * A {@linkplain CargoChain#buildAndCommit() build-n-commit} run.
 */
public class CommitRun extends Run {
  
  
  private long preCommitNo;
  private long postCommitNo;
  private int crumCount;

  public CommitRun(CargoChain cargoChain) {
    super(cargoChain);
  }

  
  

  /**
   * Builds and commits the committable cargo blocks into
   * the time chain. {@linkplain #preCommitNo() pre-commit}-,
   * {@linkplain #postCommitNo() post-commit}-no.s, and
   * {@linkplain #crumCount() crum-count} are first cleared,
   * then updated on return.
   * 
   * @see CargoChain#buildAndCommit()
   */
  @Override
  protected void runImpl() {
    synchronized (this) {
      preCommitNo = postCommitNo = crumCount = 0;
      this.preCommitNo = cargoChain.timechain().size();
    }
    int crums = cargoChain.buildAndCommit();
    long commitNo = cargoChain.timechain().size();
    synchronized (this) {
      this.crumCount = crums;
      this.postCommitNo = commitNo;
    }
  }
  
  
  /**
   * @return {@link #postCommitNo()} &gt; {@link #preCommitNo()}
   */
  public synchronized boolean advanced() {
    return postCommitNo > preCommitNo;
  }
  
  
  /** Number of crums committed in the last run. */
  public synchronized int crumCount() {
    return crumCount;
  }
  
  /** Time chain's block-count before the run. */
  public synchronized long preCommitNo() {
    return preCommitNo;
  }
  

  /** Time chain's block-count after the run. */
  public synchronized long postCommitNo() {
    return postCommitNo;
  }

}







