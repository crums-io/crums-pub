/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.Constants;
import io.crums.tc.Crum;
import io.crums.tc.Crumtrail;
import io.crums.tc.TimeBinner;
import io.crums.tc.json.CrumtrailParser;
import io.crums.testing.IoTestCase;
import io.crums.util.IntegralStrings;
import io.crums.util.json.JsonPrinter;

import org.junit.jupiter.api.Test;



/**
 * Note these tests appear repeatable, but in truth
 * they're not. They're very much sensitive to timing
 * and assume reasonably fast i/o.
 */
public class NotaryTest extends IoTestCase {

  
  @Test
  public void testIncept() throws Exception {
    
    final Object label = new Object() { };
    final TimeBinner binner = TimeBinner.SEC_4;
    final long now = System.currentTimeMillis();
    final int blocksRetained = 3;

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");
    
    
    var notary = Notary.incept(
          dir,
          binner,
          now,
          blocksRetained);
    
    
    assertTrue(notary.isOpen());
    assertEquals(0L, notary.blockCount());
    out.println("time chain file: " + notary.timechain().file());
    out.println("chain params: " + notary.timechain().params());
    
    notary.close();
    assertFalse(notary.isOpen());
    
    var rt = Notary.load(dir);

    assertTrue(rt.isOpen());
    assertEquals(0L, rt.blockCount());
    assertTrue(notary.settings().equalSettings(rt.settings()));
    
    rt.close();
    assertFalse(rt.isOpen());
  }
  
  

  @Test
  public void testOneUncommitted() throws Exception {
    
    final Object label = new Object() { };
    final TimeBinner binner = TimeBinner.SEC_4;
    final long startUtc = System.currentTimeMillis();
    final int blocksRetained = 3;
    final Random random = new Random(210L);

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");
    
    var notary = Notary.incept(
          dir,
          binner,
          startUtc,
          blocksRetained);
    
    
    out.println("chain params:   " + notary.timechain().params());
    
    byte[] rhash = new byte[Constants.HASH_WIDTH];
    random.nextBytes(rhash);
    var hash = ByteBuffer.wrap(rhash).asReadOnlyBuffer();
    out.println("witnessed hash: " + IntegralStrings.toHex(rhash));
    var rcpt = notary.witness(hash);
    assertEquals(0L, notary.blockCount());
    
    assertEquals(Constants.HASH_WIDTH, hash.remaining());
    assertEquals(hash, rcpt.crum().hash());
    assertFalse(rcpt.hasTrail());
    assertNull(rcpt.trail());
    assertTrue(rcpt.trailOpt().isEmpty());
    
    
    var uRcpt = notary.update(rcpt.crum());
    assertEquals(0L, notary.blockCount());
    
    assertEquals(hash, uRcpt.crum().hash());
    assertEquals(rcpt.crum().utc(), uRcpt.crum().utc());
    assertFalse(uRcpt.hasTrail());
    
    notary.close();
    assertFalse(notary.isOpen());
    
    var rt = Notary.load(dir);
    
    assertTrue(rt.isOpen());
    assertEquals(0L, rt.blockCount());
    
    var reloadedRcpt = rt.witness(hash);
    assertEquals(hash, reloadedRcpt.crum().hash());
    assertEquals(rcpt.crum().utc(), reloadedRcpt.crum().utc());
    assertFalse(reloadedRcpt.hasTrail());
    
    rt.close();
    assertFalse(rt.isOpen());
    
  }
  
  
  
  @Test
  public void testOneCommitted() throws Exception {
    
    final Object label = new Object() { };
    final TimeBinner binner = CargoChain.FINEST_BINNER;
    final long startUtc = System.currentTimeMillis();
    final int blocksRetained = 12;
    final Random random = new Random(210L);

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");

    
    var notary = Notary.incept(
          dir,
          binner,
          startUtc,
          blocksRetained);
    
    
    out.println("chain params:   " + notary.timechain().params());
    
    byte[] rhash = new byte[Constants.HASH_WIDTH];
    random.nextBytes(rhash);
    var hash = ByteBuffer.wrap(rhash).asReadOnlyBuffer();
    
    var crum = notary.witness(hash).crum();
    out.println("witnessed hash: " + IntegralStrings.toHex(rhash));
    assertEquals(hash, crum.hash());
    Thread.sleep(3 * binner.duration());
    int crumsAdded = notary.cargoChain.build();
    assertEquals(1, crumsAdded);
    assertTrue(notary.blockCount() > 0);
    
    var rcpt = notary.update(crum);
    assertTrue(rcpt.hasTrail());
    notary.close();
    
    var trail = rcpt.trail();
    assertEquals(crum, trail.crum());
    
    print(trail);
    
    notary = Notary.load(dir);
    rcpt = notary.witness(crum.hash());
    assertTrue(rcpt.hasTrail());
    assertEquals(crum, rcpt.trail().crum());
  }
  
  
  
  @Test
  public void testTwoUncommitted() throws Exception {
    
    final Object label = new Object() { };
    final TimeBinner binner = CargoChain.FINEST_BINNER;
    final long startUtc = System.currentTimeMillis();
    final int blocksRetained = 12;
    final Random random = new Random(210L);
    final int crumCount = 2;

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");

    
    List<ByteBuffer> wHashes = new ArrayList<>();
    for (int count = crumCount; count-- > 0; ) {
      byte[] rhash = new byte[Constants.HASH_WIDTH];
      random.nextBytes(rhash);
      wHashes.add(ByteBuffer.wrap(rhash));
    }
    
    var notary = Notary.incept(
          dir,
          binner,
          startUtc,
          blocksRetained);
    
    List<Receipt> receipts = new ArrayList<>();
    for (int index = 0; index < crumCount; ++index) {
      var rcpt = notary.witness(wHashes.get(index).slice());
      assertFalse(rcpt.hasTrail());
      assertEquals(wHashes.get(index), rcpt.crum().hash());
      receipts.add(rcpt);
    }
    
    out.println("chain params:   " + notary.timechain().params());
    notary.close();
    
    notary = Notary.load(dir);
    for (int index = 0; index < crumCount; ++index) {
      Crum expected = receipts.get(index).crum();
      var rt = notary.update(expected);
      assertEquals(expected, rt.crum());
      assertFalse(rt.hasTrail());
      rt = notary.witness(expected.hash());
      assertFalse(rt.hasTrail());
      assertEquals(expected, rt.crum());
    }
    
    assertEquals(0L, notary.blockCount());
    
    notary.close();
  }
  
  

  @Test
  public void testTwoCommitted() throws Exception {
    
    final Object label = new Object() { };
    final TimeBinner binner = CargoChain.FINEST_BINNER;
    final long startUtc = System.currentTimeMillis();
    final int blocksRetained = 12;
    final Random random = new Random(210L);
    final int crumCount = 2;

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");

    
    List<ByteBuffer> wHashes = new ArrayList<>();
    for (int count = crumCount; count-- > 0; ) {
      byte[] rhash = new byte[Constants.HASH_WIDTH];
      random.nextBytes(rhash);
      wHashes.add(ByteBuffer.wrap(rhash));
    }
    
    var notary = Notary.incept(
          dir,
          binner,
          startUtc,
          blocksRetained);
    
    List<Receipt> receipts = new ArrayList<>();
    for (int index = 0; index < crumCount; ++index) {
      var rcpt = notary.witness(wHashes.get(index).slice());
      assertFalse(rcpt.hasTrail());
      assertEquals(wHashes.get(index), rcpt.crum().hash());
      receipts.add(rcpt);
    }
    
    
    out.println("chain params:   " + notary.timechain().params());
    
    Thread.sleep(4 * binner.duration());
    assertEquals(0L, notary.blockCount());
    int crumsCommitted = notary.cargoChain.build();
    assertTrue(notary.blockCount() > 0);
    assertEquals(crumCount, crumsCommitted);
    
    for (var rcpt : receipts) {
      var uRcpt = notary.update(rcpt.crum());
      assertTrue(uRcpt.hasTrail());
      assertEquals(rcpt.crum(), uRcpt.crum());
      print(uRcpt.trail());
    }
    
    notary.close();
    
    
  }
  
  @Test
  public void testTwoCommits() throws Exception {
    
    final Object label = new Object() { };
    final TimeBinner binner = CargoChain.FINEST_BINNER;
    final long startUtc = System.currentTimeMillis();
    final int blocksRetained = 12;
    final Random random = new Random(210L);
    final int crumCount = 2;

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");

    
    List<ByteBuffer> wHashes = new ArrayList<>();
    for (int count = crumCount; count-- > 0; ) {
      byte[] rhash = new byte[Constants.HASH_WIDTH];
      random.nextBytes(rhash);
      wHashes.add(ByteBuffer.wrap(rhash));
    }
    
    var notary = Notary.incept(
          dir,
          binner,
          startUtc,
          blocksRetained);

    out.println("chain params:   " + notary.timechain().params());
    
    var rcpt = notary.witness(wHashes.get(0).slice());
    
    Thread.sleep(3 * binner.duration());
    
    int crumsCommitted = notary.cargoChain.build();
    assertEquals(1, crumsCommitted);
    
    rcpt = notary.update(rcpt.crum());
    assertTrue(rcpt.hasTrail());
    
    var rcpt2 = notary.witness(wHashes.get(1).slice());
    assertFalse(rcpt2.hasTrail());
    
    Thread.sleep(3 * binner.duration());
    
    crumsCommitted = notary.cargoChain.build();
    assertEquals(1, crumsCommitted);
    
    rcpt2 = notary.update(rcpt2.crum());
    assertTrue(rcpt2.hasTrail());
    
    assertEquals(wHashes.get(0), rcpt.crum().hash());
    assertEquals(wHashes.get(1), rcpt2.crum().hash());
    
    notary.close();
    
  }
  
  
  @Test
  public void testOneThousand() throws Exception {

    
    final Object label = new Object() { };
    final TimeBinner binner = TimeBinner.MILLIS_64;
    final long startUtc = System.currentTimeMillis();
    final int blocksRetained = 12;
    final Random random = new Random(210L);
    final int crumCount = 1000;

    final File dir = newMethodRunDir(label);
    
    final String name = method(label);
    
    var out = System.out;
    out.println();
    out.println(name + ":");

    
    List<ByteBuffer> wHashes = new ArrayList<>();
    for (int count = crumCount; count-- > 0; ) {
      byte[] rhash = new byte[Constants.HASH_WIDTH];
      random.nextBytes(rhash);
      wHashes.add(ByteBuffer.wrap(rhash));
    }
    
    var notary = Notary.incept(
          dir,
          binner,
          startUtc,
          blocksRetained);
    
    List<Receipt> receipts = new ArrayList<>();
    for (int index = 0; index < crumCount; ++index) {
      var rcpt = notary.witness(wHashes.get(index).slice());
      receipts.add(rcpt);
    }
    
    
    long lastUtc = 0;
    for (int index = 0; index < crumCount; ++index) {
      var rcpt = receipts.get(index);
      assertFalse(rcpt.hasTrail());
      assertEquals(wHashes.get(index), rcpt.crum().hash());
      assertTrue(rcpt.crum().utc() >= lastUtc);
      lastUtc = rcpt.crum().utc();
    }
    
    
    out.println("chain params:   " + notary.timechain().params());
    
    long lastBlockUtcEnd =
        binner.binTime(lastUtc) + binner.duration();
    
    long commitSlack = 3 * binner.duration();
    
    final long targetCommitUtc = lastBlockUtcEnd + commitSlack;
    
    long now = System.currentTimeMillis();
    
    if (now < targetCommitUtc)
      Thread.sleep(targetCommitUtc - now);
    assertEquals(0L, notary.blockCount());
    int crumsCommitted = notary.cargoChain.build();
    assertTrue(notary.blockCount() > 0);
    assertEquals(crumCount, crumsCommitted);
    
    long lap = System.currentTimeMillis() - now;
    out.println("built and committed in " + lap + " ms");
    
    now += lap;
    
    Receipt r500 = null;
    for (int index = 0; index < crumCount; ++index) {
      var rcpt = receipts.get(index);
      var uRcpt = notary.update(rcpt.crum());
      assertTrue(uRcpt.hasTrail());
      assertEquals(rcpt.crum(), uRcpt.crum());
      if (index == 500)
        r500 = uRcpt;
    }
    
    lap = System.currentTimeMillis() - now;
    out.println("retrieved and verified in " + lap + " ms");
    
    out.println("sample trail..");
    print(r500.trail());
    notary.close();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  private void print(Crumtrail trail) {
    JsonPrinter.println(
        new CrumtrailParser(HashEncoding.HEX).toJsonObject(trail));
  }
  
  

}




















