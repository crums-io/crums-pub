/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;


import java.nio.channels.Channel;

import io.crums.tc.ChainParams;

/**
 * Managed {@code Runnable} task.
 * 
 * @see #run()
 * @see #stop()
 * @see #isOpen()
 * @see #sleepMillis()
 * @see Run
 */
public class Daemon<T extends Run> implements Runnable, Channel {
  
  /**
   * As a sanity check, no Daemon can run anything less frequently than
   * once a year.
   */
  public final long MAX_SLEEP = 365 * 24 * 3600 * 1000;
  
  /**
   * <h4>Note</h4>
   * Lock held (synchronized section) while waiting (sleeping).
   * Reason why we wait ({@code Object.wait(..)}), instead of sleep
   * {@code Thread.sleep(..)} is to facilitate wake-ups, shutdowns or
   * other "out-of-band" procedures: interrupting the waiting thread
   * via this object's monitor is easier than finding the thread and
   * directly interrupting it.
   */
  protected final Object lock = new Object();
  
  protected final T job;
  
  // private int readyCount;
  private long runCount;
  private long successCount;
  private volatile boolean stop;
  
  
  // private void clear() {
  //   runCount = successCount = 0;
  //   stop = false;
  // }
  
  
  
  
  public Daemon(T job) {
    this.job = job;
    if (job.blockFrequency() <= 0.01 ||
        sleepMillis() == 0 ||
        sleepMillis() > MAX_SLEEP)
      
      throw new IllegalArgumentException(
          job + ".blockFrequency(): " + job.blockFrequency() +
          "; " + chainParams());
      
  }
  
  
  private ChainParams chainParams() {
    return job.cargoChain.settings().chainParams();
  }

  /**
   * Runs the loop while {@linkplain #isOpen() open}. In each
   * round of the loop, the underlying {@linkplain #getJob() job}
   * is first run, and then the thread waits {@linkplain #sleepMillis()},
   * before the next run.
   * 
   */
  @Override
  public void run() {
    
    if (!isOpen())
      throw new IllegalStateException(this + " is closed");
    
    job.log().info("Starting daemon: " + job.name());


    while (isOpen()) {

      // run the job
      try {
        job.run();
        ++runCount;
        if (job.succeeded())
          ++successCount;
        else if (job.hasException()) {
          job.log().error(job.name() + " failed: " + job.getException() + " exiting");
          break;
        }

      } catch (Exception x) {
        job.log().error(
            job.name() +
            " encountered an error. Stopping daemon. Detail: " + x);
        break;
      }

      // sleep
      try {
        synchronized (lock) {
          if (stop)
            break;
          lock.wait(sleepMillis());
        }
      } catch (InterruptedException ix) {
        Thread.interrupted();
        job.log().info(job.name() + " exiting via interrupt");
        break;
      }
    }
    
  }
  
  
  /**
   * The {@code Run} this instance was constructed with.
   * 
   * @return a {@linkplain Run}
   */
  public T getJob() {
    return job;
  }
  
  
  /**
   * Milliseconds waited.
   * 
   * @return defaults to
   *  {@code (long) (chainParams().blockDuration() / getJob().blockFrequency()}
   */
  public long sleepMillis() {
    return (long) (chainParams().blockDuration() / job.blockFrequency());
  }
  
  
  
  /** Signals the daemon thread to stop. */
  public void stop() {
    stop = true;
    synchronized (lock) {
      lock.notifyAll();
    }
  }
  
  
  /** Returns the number of times the job has run. */
  public long runCount() {
    return runCount;
  }
  
  /** Returns the number of times the job has run and succeeded. */
  public long successCount() {
    return successCount;
  }
  
  
  /**
   * An instance is open until {@linkplain #stop() stop}ped.
   */
  @Override
  public final boolean isOpen() {
    return !stop;
  }
  
  
  /** Synonym for {@link #stop()}. */
  @Override
  public void close() {
    stop();
  }


  @Override
  public String toString() {
    return "Daemon<" + job.name() + ">";
  }

}

