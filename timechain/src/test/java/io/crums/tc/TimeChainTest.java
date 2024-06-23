/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import io.crums.sldg.Path;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * 
 */
public class TimeChainTest extends TimeChainTestCase {

  
  @Test
  public void testEmpty() throws Exception {
    final Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label, "empty");
    var binner = TimeBinner.MINUTE;
    TimeChain chain = TimeChain.inceptNewChain(chainFile, binner);
    final long now = System.currentTimeMillis();
    final long inceptUtc = chain.params().inceptionUtc();
    
    assertTrue(chain.isOpen());
    assertFalse(chain.isReadOnly());
    assertTrue(chain.isEmpty());
    assertEquals(0L, chain.blockCount());
    assertEquals(binner, chain.params().timeBinner());
    assertTrue(now > inceptUtc);
    assertTrue(now < inceptUtc + 2 * binner.duration());
    
    chain.close();
    assertFalse(chain.isOpen());
    
    chain = TimeChain.load(chainFile, true);
    assertTrue(chain.isOpen());
    assertTrue(chain.isEmpty());
    assertTrue(chain.isReadOnly());
    assertEquals(0L, chain.blockCount());
    assertEquals(binner, chain.params().timeBinner());
    assertEquals(inceptUtc, chain.params().inceptionUtc());

    chain.close();
    assertFalse(chain.isOpen());
  }
  
  
  @Test
  public void testOne() throws Exception {
    final Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label, "one");
    var binner = TimeBinner.MINUTE;
    
    ByteBuffer mockHash;
    {
      Random rand = new Random(11L);
      byte[] mh = new byte[Constants.HASH_WIDTH];
      rand.nextBytes(mh);
      mockHash = ByteBuffer.wrap(mh).asReadOnlyBuffer();
    }
    
    TimeChain chain = TimeChain.inceptNewChain(chainFile, binner);
    final long now = System.currentTimeMillis();
    
    assertTrue(chain.isEmpty());
    
    long blocksAdded = chain.recordBlockForUtc(
        now, mockHash.slice());
    
    assertTrue(blocksAdded >= 1);
    assertEquals(blocksAdded, chain.blockCount());
    
    final Path state = chain.statePath();
    assertEquals(mockHash.slice(), state.last().inputHash());
    
    chain.close();
    
    chain = TimeChain.load(chainFile, true);
    
    var block = chain.getBlockForUtc(now);
    assertEquals(mockHash.slice(), block.cargoHash());
    
    assertEquals(state, chain.statePath());
    
    chain.close();
  }
  
  
  @Test
  public void testTwo() throws Exception {
    final Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label, "two");
    var binner = TimeBinner.MINUTE;
    long seed = 33L;
    
    testRun(2, chainFile, binner, seed);
  }
  

  
  
  @Test
  public void testThree() throws Exception {
    final Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label, "three");
    var binner = TimeBinner.EIGTH_SEC;
    long seed = 33L;
    
    testRun(3, chainFile, binner, seed);
  }
  
  
  @Test
  public void test100() throws Exception {
    final Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label, "hundred");
    var binner = TimeBinner.MILLIS_64;
    long seed = 33L;
    
    testRun(100, chainFile, binner, seed, true);
    
  }
  
  
  @Test
  public void test1000() throws Exception {
    final Object label = new Object() { };
    if (!checkEnabled(TEST_ALL, label))
      return;
    
    File chainFile = newSingleRunFilepath(label, "thou");
    var binner = TimeBinner.MILLIS_64;
    long seed = 33L;
    
    testRun(1000, chainFile, binner, seed, true);
    
  }
  
  private void testRun(
      final int blocks, File chainFile, TimeBinner binner, long seed)
      throws Exception {
    testRun(blocks, chainFile, binner, seed, false);
  }
  
  
  private void testRun(
      final int blocks, File chainFile, TimeBinner binner, long seed,
      boolean verbose)
      throws Exception {

    Random rand = new Random(seed);
    

    if (verbose) {
      System.out.println();
      System.out.print("generating " + blocks + " random hashes.. ");
      System.out.flush();
    }
    

    long perfNow = System.currentTimeMillis();
    
    var cargoHashes = new ArrayList<ByteBuffer>(blocks);
    {
      for (int countdown = blocks; countdown-- > 0; ) {
        byte[] mockHash = new byte[Constants.HASH_WIDTH];
        rand.nextBytes(mockHash);
        cargoHashes.add(ByteBuffer.wrap(mockHash));
      }
    }
    
    if (verbose) {
      long lap = System.currentTimeMillis() - perfNow;
      System.out.println("Done. (" + lap + " ms)");
      perfNow += lap;
      System.out.print("adding " + blocks + " blocks.. ");
      System.out.flush();
    }
    
    final long now = System.currentTimeMillis();
    final long startUtc = now - (blocks + 1) * binner.duration();
    
    var chain = TimeChain.inceptNewChain(chainFile, binner, startUtc);
    
    long tally = 0;
    for (int index = 0; index < blocks; ++index) {
      long utc = startUtc + binner.duration() * index;
      tally += chain.recordBlockForUtc(
          utc,
          cargoHashes.get(index));
    }
    
    assertEquals(tally, chain.blockCount());
    

    if (verbose) {
      long lap = System.currentTimeMillis() - perfNow;
      perfNow += lap;
      System.out.println("Done. (" + lap + " ms)");
      System.out.print("retrieving chain state.. ");
      System.out.flush();
    }
    
    final Path chainState = chain.statePath().pack().path();
    
    chain.close();

    if (verbose) {
      long lap = System.currentTimeMillis() - perfNow;
      perfNow += lap;
      System.out.println("Done. (" + lap + " ms)");
      System.out.print("reloading chain state.. ");
      System.out.flush();
    }
    
    chain = TimeChain.load(chainFile, true);
    
    assertEquals(chainState, chain.statePath());

    if (verbose) {
      long lap = System.currentTimeMillis() - perfNow;
      System.out.println("Done. (" + lap + " ms)");
    }
    chain.close();
  }

}
