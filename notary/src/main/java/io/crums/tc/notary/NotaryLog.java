/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;

import java.lang.System.Logger.Level;

/**
 * Adaptor for log messages in this library.
 */
public abstract class NotaryLog {
  
  /** Adaptor for {@code System.Logger}. */
  public final static NotaryLog SYS = new Sys();
  /** Does not log. */
  public final static NotaryLog NULL = new Null();


  public abstract void info(String msg);
  public abstract void warning(String msg);
  public abstract void fatal(Throwable error);
  
  
  
  public final static class Sys extends NotaryLog {
    
    private final System.Logger logger;
    
    
    public Sys() {
      this.logger = System.getLogger(NotaryConstants.NOTARY_LOG);
    }

    @Override
    public void info(String msg) {
      logger.log(Level.INFO, msg);
    }

    @Override
    public void warning(String msg) {
      logger.log(Level.WARNING, msg);
      
    }

    @Override
    public void fatal(Throwable error) {
      logger.log(Level.ERROR, error.toString());
    }
  }
  
  /** So not anomymous. */
  private final static class Null extends NotaryLog {
    @Override
    public void info(String msg) {  }
    @Override
    public void warning(String msg) {  }
    @Override
    public void fatal(Throwable error) {  }
  }
  

}
