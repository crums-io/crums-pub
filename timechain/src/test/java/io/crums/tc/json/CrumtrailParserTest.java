/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;


import static io.crums.tc.CrumtrailTest.newRandomCrumtrail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.Crumtrail;
import io.crums.tc.CrumtrailTest.RandArgs;
import io.crums.tc.HashUtc;
import io.crums.tc.TimeBinner;
import io.crums.testing.SelfAwareTestCase;
import io.crums.util.json.JsonPrinter;


/**
 * 
 */
public class CrumtrailParserTest extends SelfAwareTestCase {
  
  
  
  private final static CrumtrailParser B64 =
      new CrumtrailParser(HashEncoding.BASE64_32);
  
  
  
  
  @Test
  public void testMinimal() {
    Object label = new Object() { };
    int blockNo = 1;
    int chainLen = 1;
    int blockCrums = 1;
    TimeBinner binner = TimeBinner.SEC_8;
    long randSeed = 38;
    
    testRoundtrip(
        new RandArgs(
            blockNo, chainLen, blockCrums, binner, randSeed),
        B64, label);
  }
  

  
  
  @Test
  public void testBlockOne73Crums() {
    Object label = new Object() { };
    int blockNo = 1;
    int chainLen = 1;
    int blockCrums = 74;
    TimeBinner binner = TimeBinner.SEC_8;
    long randSeed = 38;
    
    testRoundtrip(
        new RandArgs(
            blockNo, chainLen, blockCrums, binner, randSeed),
        B64, label);
  }
  
  
  public void testMany() {
    Object label = new Object() { };
    var binner = TimeBinner.EIGTH_SEC;
    long seed = HashUtc.INCEPTION_UTC;
    RandArgs[] silent = {
        new RandArgs(1, 2, 1, binner, seed),
        new RandArgs(1, 2, 99, binner, seed),
        new RandArgs(2, 2, 1, binner, seed),
        new RandArgs(2, 2, 55, binner, seed),
        new RandArgs(1, 3, 1, binner, seed),
        new RandArgs(1, 3, 9, binner, seed),
        new RandArgs(2, 3, 1, binner, seed),
        new RandArgs(2, 3, 80, binner, seed),
        new RandArgs(3, 3, 1, binner, seed),
        new RandArgs(3, 3, 86, binner, seed),
    };
    
    RandArgs[] chatty = {
        new RandArgs(3080, 34_067, 1, binner, seed),
        new RandArgs(3080, 34_067, 289, binner, seed),
    };
    
    for (var args : silent)
      testRoundtrip(args, B64);
    for (var args : chatty)
      testRoundtrip(args, B64, label);
  }
  
  private void testRoundtrip(RandArgs args, CrumtrailParser parser) {
    testRoundtrip(args, parser, null);
  }
  
  private void testRoundtrip(RandArgs args, CrumtrailParser parser, Object label) {
    
    Crumtrail expected = newRandomCrumtrail(args);
    var jObj = parser.toJsonObject(expected);
    if (label != null) {
      var out = System.out;
      out.println();
      out.println(method(label) + ":");
      JsonPrinter.println(jObj);
    }
    Crumtrail rt = parser.toEntity(jObj);
    assertEquals(expected.blockNo(), rt.blockNo());
    assertEquals(expected.blockProof(), rt.blockProof());
    assertEquals(expected.crumsInBlock(), rt.crumsInBlock());
    assertEquals(expected.cargoHash(), rt.cargoHash());
    assertEquals(expected.crum(), rt.crum());
    if (expected.isMerkled()) {
      var xpmt = expected.asMerkleTrail();
      var rtmt = rt.asMerkleTrail();
      assertEquals(xpmt.cargoHash(), rtmt.cargoHash());
      assertEquals(
          ByteBuffer.wrap(xpmt.cargoProof().rootHash()),
          ByteBuffer.wrap(rtmt.cargoProof().rootHash()));
    }
  }

}
