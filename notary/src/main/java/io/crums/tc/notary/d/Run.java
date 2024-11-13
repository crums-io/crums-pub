/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.d;

import java.util.Objects;

import io.crums.tc.notary.CargoChain;
import io.crums.tc.notary.NotaryLog;

/**
 * Base run job factored out of 2 similar classes.
 * Instances are designed to be run multiple times.
 * 
 */
public abstract class Run implements Runnable {
  
  
  protected final CargoChain cargoChain;
  
  private volatile Exception error;
  
  
  protected Run(CargoChain cargoChain) {
    this.cargoChain = Objects.requireNonNull(cargoChain);
  }


  
  /**
   * Invokes {@linkplain #runImpl()} after clearing the
   * {@linkplain #getException() exception}; if {@code runImpl()}
   * throws an {@code Exception}, it is caught, recorded, and
   * (possibly) processed thru the {@linkplain #exceptionCaught(Exception)}
   * method (if overridden).
   */
  @Override
  public final void run() {
    
    error = null;
    
    if (!cargoChain.isOpen()) {
      cargoChain.log().error(
        "Cargo chain is closed.. " + getClass().getSimpleName());
      return;
    }
    
    try {
      runImpl();
    } catch (Exception x) {
      this.error = x;
      exceptionCaught(x);
    }
  }


  public NotaryLog log() {
    return cargoChain.log();
  }
  
  
  /**
   * Subclass hook. Called when {@linkplain #runImpl()} throws.
   * Noop in base class.
   */
  protected void exceptionCaught(Exception x) {  }

  
  /**
   * Invoked by {@linkplain #run()}, inside synchronization.
   * Before invocation, any exception stored is cleared; if an
   * exception is thrown by this method, then it becomes the new
   * value for {@linkplain #getException()}.
   * 
   * @throws Exception if something goes wrong
   */
  protected abstract void runImpl() throws Exception;

  

  /**
   * Average no. of times per block expected to run.
   * Must be greater than zero: a block frequency of
   * {@code 0.5} means once every other block; {@code 2}
   * means twice every block, for eg.
   * 
   * @return default value is {@code 1} (once)
   */
  public float blockFrequency() {
    return 1;
  }
  
  
  /** Returns any exception caught during the last run; {@code null}, o.w. */
  public final Exception getException() {
    return error;
  }


  public final boolean hasException() {
    return error != null;
  }



  
  /**
   * Reports whether the last run succeeded.
   * 
   * @return {@code advanced() && getException() == null}
   */
  public final boolean succeeded() {
    return advanced() && !hasException();
  }
  
  
  /**
   * Determines whether the last run advanced the state of whatever it was
   * attempting to update (or <em>appeared to advance</em>, in the event
   * of a race with another thread of execution).
   */
  public abstract boolean advanced();
  
  
  /**
   * Returns the job name.
   * 
   * @return {@code getClass().getSimpleName()}
   */
  public String name() {
    return getClass().getSimpleName();
  }

}
