/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;


import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.Objects;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.util.mrkl.Builder;
import io.crums.util.mrkl.FixedLeafBuilder;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.Tree;

/**
 * 
 */
public class CargoProofTest {
  
  

  public static CargoProof randomCargoProof(int index, int leafCount) {
    return randomCargoProof(index, leafCount, 1793604800123L);
  }
  
  
  /**
   * Randomization is based on leaf count.
   */
  public static CargoProof randomCargoProof(int index, int leafCount, long utc) {
    return randomCargoProof(index, leafCount, utc, new Random(leafCount));
  }
  
  
  public static CargoProof randomCargoProofForUtc(
      int leafCount, long utc, Random random) {
    int index = random.nextInt(leafCount);
    return randomCargoProof(index, leafCount, utc, random);
  }
  
  
  public static CargoProof randomCargoProof(
      int index, int leafCount, long utc, Random random) {
    assertTrue(leafCount > 1);
    Objects.checkIndex(index, leafCount);
    byte[] hash = new byte[Constants.HASH_WIDTH];
    random.nextBytes(hash);
    
    Crum crum = new Crum(hash, utc);
    
    MessageDigest digest = Constants.DIGEST.newDigest();
    digest.update(crum.serialForm());
    
    byte[] hashOfCrum = digest.digest();

    Builder builder = new FixedLeafBuilder(Constants.HASH_ALGO);
    
    for (int i = 0; i < index; ++i) {
      random.nextBytes(hash);
      builder.add(hash);
    }
    builder.add(hashOfCrum);
    for (int i = index + 1; i < leafCount; ++i) {
      random.nextBytes(hash);
      builder.add(hash);
    }
    
    Tree tree = builder.build();
    Proof proof = new Proof(tree, index);
    return new CargoProof(proof, crum);
  }
  
  
  @Test
  public void testOne() {

    int leafCount = 129_083;
    int index = (leafCount/3) * 2;
    
    CargoProof cargoProof = randomCargoProof(index, leafCount);
    assertEquals(index, cargoProof.leafIndex());
    assertEquals(leafCount, cargoProof.leafCount());
    
  }
  
  
  @Test
  public void testSerialRoundtrip() {

    int leafCount = 129_083;
    int index = (leafCount/3) * 2 - 1;
    
    CargoProof cargoProof = randomCargoProof(index, leafCount);
    
    var buffer = cargoProof.serialize();
    CargoProof rt = CargoProof.load(buffer);
    
    assertEquals(cargoProof, rt);
    assertEquals(cargoProof.crum(), rt.crum());
    
  }

}
