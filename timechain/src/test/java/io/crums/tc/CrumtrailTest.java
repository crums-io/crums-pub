/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;


import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.sldg.CompactSkipLedger;
import io.crums.sldg.SkipLedger;
import io.crums.sldg.VolatileTable;


/**
 * 
 */
public class CrumtrailTest extends TimeChainTestCase {
  


  public static SkipLedger newRandomLedger(int rows) {
    if (rows <= 0)
      throw new IllegalArgumentException("rows " + rows);
    
    SkipLedger ledger = newLedger();
    
    Random random = new Random(rows);
    
    addRandomRows(ledger, random, rows);
    
    assertEquals(rows, ledger.size());
    return ledger;
  }
  
  
  public static SkipLedger newLedger() {
    return new CompactSkipLedger(new VolatileTable());
  }
  
  
  public static void addRandomRows(SkipLedger ledger, Random random, int count) {

    byte[] mockHash = new byte[Constants.HASH_WIDTH];
    ByteBuffer mockHashBuffer = ByteBuffer.wrap(mockHash);
    
    for (; count-- > 0; ) {
      random.nextBytes(mockHash);
      mockHashBuffer.clear();
      ledger.appendRows(mockHashBuffer);
    }
  }

  
  /**
   * Chunkified args for {@linkplain CrumtrailTest#newRandomCrumtrail(RandArgs)}
   * method.
   */
  public record RandArgs(
      int blockNo, int chainLen, int blockCrums, TimeBinner binner, Random rand) {
    
    public RandArgs {
      assertTrue(blockNo > 0);
      assertTrue(chainLen >= blockNo);
      assertTrue(blockCrums > 0);
      assertNotNull(rand);
    }
    
    public RandArgs(
        int blockNo, int chainLen, int blockCrums, TimeBinner binner, long seed) {
      this(blockNo, chainLen, blockCrums, binner, new Random(seed));
    }
  }
  
  
  
  public static Crumtrail newRandomCrumtrail(RandArgs args) {
    var mockChain = newLedger();
    addRandomRows(mockChain, args.rand, args.blockNo - 1);
    ChainParams params;
    {
      long hrAgo = System.currentTimeMillis() - 3_600_000;
      long rewind = args.binner.duration() * args.chainLen;
      long inceptUt = args.binner.binTime(hrAgo - rewind);
      params = new ChainParams(args.binner, inceptUt);
    }
    final long utc = crumUtc(args.blockNo, params, args.rand);
    Crum crum;
    CargoProof cargoProof;
    ByteBuffer cargoHash;
    if (args.blockCrums == 1) {
      cargoProof = null;
      byte[] hash = new byte[Constants.HASH_WIDTH];
      args.rand.nextBytes(hash);
      crum = new Crum(hash, utc);
      cargoHash = ByteBuffer.wrap(crum.witnessHash()).asReadOnlyBuffer();
    } else {
      cargoProof = CargoProofTest.randomCargoProofForUtc(
          args.blockCrums, utc, args.rand);
      crum = cargoProof.crum();
      cargoHash = ByteBuffer.wrap(cargoProof.rootHash()).asReadOnlyBuffer();
    }
    mockChain.appendRows(cargoHash);
    addRandomRows(mockChain, args.rand, args.chainLen - args.blockNo);
    
    assertEquals(args.chainLen, mockChain.size());
    var path = args.blockNo == args.chainLen || args.blockNo == 1 ?
        mockChain.statePath() :
          mockChain.getPath(1L, (long) args.blockNo, mockChain.size());
    
    BlockProof blockProof = new BlockProof(params, path);
    return cargoProof == null ?
        Crumtrail.newLoneTrail(blockProof, crum) :
          Crumtrail.newMerkleTrail(blockProof, cargoProof);
  }

  
  private static long crumUtc(int blockNo, ChainParams params, Random rand) {
    return
        params.utcForBlockNo(blockNo) +
        rand.nextLong(params.timeBinner().duration());
  }

  
  @Test
  public void testLoneTrail() throws Exception {
    Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label);
    var binner = TimeBinner.MINUTE;
    
    final long blockCount = 139;
    final long blockNo = 44;
    
    Random rand = new Random(96);
    
    final long startUtc =
        4392 + binner.binTime(
            System.currentTimeMillis() - binner.duration() * (blockCount + 1));
    

    
    final long crumUtc = startUtc + (blockNo - 1) * binner.duration();
    
    byte[] crumHash = new byte[Constants.HASH_WIDTH];
    rand.nextBytes(crumHash);
    
    final var crum = new Crum(crumHash, crumUtc);

    try (var chain = TimeChain.inceptNewChain(chainFile, binner, startUtc)) {

      for (int b = 1; b < blockNo; ++b) {
        byte[] mockHash = new byte[Constants.HASH_WIDTH];
        rand.nextBytes(mockHash);
        long utc = startUtc + (b - 1) * binner.duration();
        chain.recordBlockForUtc(
            utc,
            ByteBuffer.wrap(mockHash),
            1 + b % 5);   // whatever
      }
      chain.recordBlockForUtc(crumUtc, crum.witnessHash(), 1);
      
      for (long b = blockNo + 1; b <= blockCount; ++b) {
        byte[] mockHash = new byte[Constants.HASH_WIDTH];
        rand.nextBytes(mockHash);
        long utc = startUtc + (b - 1) * binner.duration();
        chain.recordBlockForUtc(
            utc,
            ByteBuffer.wrap(mockHash),
            1 + (int) b % 5);   // whatever
      }
    }
    
    try (var chain = TimeChain.load(chainFile, true)) {
      var path = chain.getPath(1L, blockNo, blockCount);
      var blockProof = new BlockProof(chain.params(), path);
      var crumtrail = Crumtrail.newLoneTrail(blockProof, crum);
      // the fact nothing blows up is good.. constructors validate, you see
      // for window dressing..
      assertEquals(blockNo, crumtrail.blockNo());
    }
    
  }
  
  
  @Test
  public void testMerkleTrail() throws Exception {
    Object label = new Object() { };
    File chainFile = newSingleRunFilepath(label);
    var binner = TimeBinner.MINUTE;
    
    final long blockCount = 77;
    final long blockNo = 17;
    
    Random rand = new Random(96);
    
    final long startUtc =
        1050 + binner.binTime(
            System.currentTimeMillis() - binner.duration() * (blockCount + 1));

    
    final long crumUtc = startUtc + (blockNo - 1) * binner.duration();
    var cargoProof = CargoProofTest.randomCargoProof(189, 1044, crumUtc);
    
    
    try (var chain = TimeChain.inceptNewChain(chainFile, binner, startUtc)) {

      for (int b = 1; b < blockNo; ++b) {
        byte[] mockHash = new byte[Constants.HASH_WIDTH];
        rand.nextBytes(mockHash);
        long utc = startUtc + (b - 1) * binner.duration();
        chain.recordBlockForUtc(
            utc,
            ByteBuffer.wrap(mockHash),
            1 + b % 5);   // whatever
      }
      
      chain.recordBlockForUtc(
          crumUtc, cargoProof.rootHash(), cargoProof.leafCount());
      
      for (long b = blockNo + 1; b <= blockCount; ++b) {
        byte[] mockHash = new byte[Constants.HASH_WIDTH];
        rand.nextBytes(mockHash);
        long utc = startUtc + (b - 1) * binner.duration();
        chain.recordBlockForUtc(
            utc,
            ByteBuffer.wrap(mockHash),
            1 + (int) b % 5);   // whatever
      }
    }
    
    try (var chain = TimeChain.load(chainFile, true)) {
      var path = chain.getPath(1L, blockNo, blockCount);
      var blockProof = new BlockProof(chain.params(), path);
      var crumtrail = Crumtrail.newMerkleTrail(blockProof, cargoProof);
      // the fact nothing blows up is good.. constructors validate, you see
      // for window dressing..
      assertEquals(cargoProof.crum(), crumtrail.crum());
    }
  }
  
  
  @Test
  public void testStatics() {
    testStatics(new RandArgs(11, 62, 1, TimeBinner.EIGTH_SEC, 5L));
    testStatics(new RandArgs(356, 1030, 67, TimeBinner.EIGTH_SEC, 10L));
  }
  
  private void testStatics(RandArgs args) {
    Crumtrail trail = newRandomCrumtrail(args);
    assertEquals(args.blockNo, trail.blockNo());
    assertEquals(args.blockCrums(), trail.crumsInBlock());
    assertEquals(args.chainLen, trail.blockProof().chainState().hiRowNumber());
    
  }

}
