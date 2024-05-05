/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;


import java.nio.channels.Channel;

import io.crums.tc.ChainParams;

/**
 * A flapping (timed) daemon.
 * 
 * @see #run()
 * @see #stop()
 * @see #isOpen()
 * @see #ready()
 * @see #sleepMillis()
 */
public class Daemon<T extends Run> implements Runnable, Channel {
  
  /**
   * As a sanity check, no Daemon can run anything less frequently than
   * once a year.
   */
  public final long MAX_SLEEP = 365 * 24 * 3600 * 1000;
  
  /**
   * Lock held (synchronized section) while waiting (sleeping).
   * Reason why we wait ({@code Object.wait(..)}), instead of sleep
   * {@code Thread.sleep(..)} is to facilitate wake-ups, shutdowns or
   * other "out-of-band" procedures: interrupting the waiting thread
   * via this object's monitor is easier than finding the thread and
   * directly interrupting it.
   */
  protected final Object lock = new Object();
  protected final T job;
  
  private int readyCount;
  private int runCount;
  private int successCount;
  private boolean stop;
  
  
  private void clear() {
    runCount = successCount = 0;
    stop = false;
    readyCount = 1;
  }
  
  
  
  
  public Daemon(T job) {
    this.job = job;
    if (job.blockFrequency() <= 0 ||
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
   * round of the loop, if {@linkplain #ready() ready}, the 
   * {@linkplain #getJob() job} is run; if not ready, then
   * the thread waits {@linkplain #sleepMillis()}, before
   * trying {@code ready()} again.
   */
  @Override
  public void run() {
    
    clear();
    
    while (isOpen()) {
      
      while (!ready()) {
        
        try {
          synchronized (lock) {
            if (stop)
              return;
            lock.wait(sleepMillis());
          }
        } catch (InterruptedException ix) {
          Thread.interrupted();
          break;  // from while (!ready
        }
        continue;
        
      } // while (!ready
      
      job.run();
      ++runCount;
      if (job.succeeded())
        ++successCount;
      
    } // while (isOpen
    
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
   * Milliseconds waited after not {@linkplain #ready() ready}.
   * 
   * @return defaults to
   *  {@code (long) (chainParams().blockDuration() / getJob().blockFrequency()}
   */
  public long sleepMillis() {
    return (long) (chainParams().blockDuration() / job.blockFrequency());
  }
  
  /**
   * Determines whether the {@linkplain Run run} is ready to execute.
   * If this method returns {@code true}, then on return the main loop
   * immediately executes (runs) the {@linkplain #getJob() job};
   * if {@code false}, then on return the current thread <em>waits</em>
   * on the {@linkplain #lock lock}'s monitor {@linkplain #sleepMillis()}
   * milliseconds.
   * 
   * <h4>Default Implementation</h4>
   * <p>
   * By default, this flaps on and off every other time invoked,
   * but stays on twice in succession every 33rd time.
   * </p>
   * <h5>Rationale</h5>
   * <p>
   * Our tasks are typically scheduled to run with a period of one
   * block duration. There are 2 considerations with regard to timing:
   * </p>
   * <ol><li>
   * The job itself takes time to complete, which in turn eats into
   * the time we must wait (the block duration).
   * </li>
   * <li>
   * Periodicity aside, the actual time a job begins (its <em>phase</em>,
   * if you will) matters. If it starts at the wrong point in the period,
   * it may settle nearly one block behind from when it should be scheduled.
   * Hence this simple jiggling.
   * </li></ol>
   */
  public boolean ready() {
    if ((++readyCount & 2) == 0) {
      if ((readyCount - 1) % 33 == 0) {
        ++readyCount;
        return false;
      }
      return true;
    }
    return readyCount % 33 == 0;
  }
  
  
  /** Signals the daemon thread to stop. */
  public void stop() {
    synchronized (lock) {
      stop = true;
      lock.notify();
    }
  }
  
  
  /** Returns the number of times the job has been run. */
  public int runCount() {
    return runCount;
  }
  
  
  /**
   * An instance is open until {@linkplain #stop() stop}ped.
   */
  @Override
  public boolean isOpen() {
    synchronized (lock) {
      return !stop;
    }
  }
  
  
  /** Synonym for {@link #stop()}. */
  @Override
  public void close() {
    stop();
  }

}
