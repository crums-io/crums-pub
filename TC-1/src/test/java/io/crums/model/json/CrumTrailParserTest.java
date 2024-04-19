/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;



import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.model.Crum;
import io.crums.model.CrumTrail;
import io.crums.util.mrkl.Builder;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.Tree;
import io.crums.model.Constants;

/**
 * 
 */
public class CrumTrailParserTest {
  

  
  @Test
  public void testRoundtrip() {
    CrumTrail trail = randomCrumTrail(3001, 2999);
    
    CrumTrailParser parser = new CrumTrailParser();
    
    String json = parser.toJsonObject(trail).toJSONString();
//    System.out.println(json);
    
    CrumTrail out = parser.toCrumTrail(json);
    
    assertEquals(trail, out);
    
    assertEquals(trail.crum(), out.crum());
  }
  

  
  
  private CrumTrail randomCrumTrail(final int leafCount, final int leafIndex) {
    return randomCrumTrail(leafCount, leafIndex, Crum.INCEPTION_UTC + 2020);
  }

  private CrumTrail randomCrumTrail(final int leafCount, final int leafIndex, long utc) {
    Random rand = new Random(53);

    Crum crum;
    {
      byte[] hash = new byte[Constants.HASH_WIDTH];
      rand.nextBytes(hash);
      
      crum = new Crum(hash, utc);
    }
    
    Builder treeBuilder = new Builder(Constants.HASH_ALGO);
    byte[][] leaves = new byte[leafCount][];
    for (int index = 0; index < leafIndex; ++index) {
      byte[] hash = new byte[Constants.HASH_WIDTH];
      rand.nextBytes(hash);
      treeBuilder.add(hash);
      leaves[index] = hash;
    }
    
    MessageDigest digester;
    try {
      digester = MessageDigest.getInstance(Constants.HASH_ALGO);
    } catch (NoSuchAlgorithmException nsax) {
      throw new AssertionError();
    }
    digester.update(crum.serialForm());
    byte[] hashOfCrum = digester.digest();
    
    treeBuilder.add(hashOfCrum);
    leaves[leafIndex] = hashOfCrum;
    
    for (int index = leafIndex + 1; index < leafCount; ++index) {
      byte[] hash = new byte[Constants.HASH_WIDTH];
      rand.nextBytes(hash);
      treeBuilder.add(hash);
      leaves[index] = hash;
    }
    
    Tree tree = treeBuilder.build();
    Proof proof = new Proof(tree, leafIndex);
    
    return new CrumTrail(proof, crum);
  }

}
