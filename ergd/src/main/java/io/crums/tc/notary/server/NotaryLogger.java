/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;


import java.util.logging.Level;
import java.util.logging.Logger;

import io.crums.tc.notary.NotaryLog;


public class NotaryLogger extends NotaryLog {

  

  private final Logger log;


  public NotaryLogger(String name) {
    this.log = Logger.getLogger(name);
  }

  @Override
  public void info(String msg) {
    log.info(msg);;
  }

  @Override
  public void warning(String msg) {
    log.warning(msg);
  }

  @Override
  public void fatal(Throwable error) {
    log.log(Level.SEVERE, error.getMessage(), error);
  }

}


