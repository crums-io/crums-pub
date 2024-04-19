/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import io.crums.testing.IoTestCase;

/**
 * 
 */
public abstract class TimeChainTestCase extends IoTestCase {

  
  public final static String TEST_ALL = "testAll";
  
  public final static String EXT = ".ctc";
  
  
  public final File newRunDirectory(Object methodLabel) {
    File dir = getMethodOutputFilepath(methodLabel);
    if (!dir.mkdirs())
      fail();
    return dir;
  }
  
  
  public final File newSingleRunFilepath(Object methodLabel) {
    return newSingleRunFilepath(methodLabel, method(methodLabel));
  }
  
  public final File newSingleRunFilepath(Object methodLabel, String name) {
    File dir = newRunDirectory(methodLabel);
    return new File(dir, name + EXT);
  }
  
  

}
