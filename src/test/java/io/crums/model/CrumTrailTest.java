/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model;


import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.testing.SelfAwareTestCase;

import io.crums.model.hashing.Statement;
import io.crums.util.hash.Digests;
import io.crums.util.mrkl.Builder;
import io.crums.util.mrkl.FixedLeafBuilder;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.Tree;


/**
 * 
 */
public class CrumTrailTest extends SelfAwareTestCase {
  
  
  @Test
  public void testToStatement() {
    
    final int leafCount = 129_083;
    final int index = (leafCount/3) * 2;
    
    Random random = new Random(leafCount);
    byte[] hash = new byte[Constants.HASH_WIDTH];
    random.nextBytes(hash);
    
    long utc = 1593604800123L;
    
    Crum crum = new Crum(hash, utc);
    
    MessageDigest digest = Digests.SHA_256.newDigest();
    digest.update(crum.serialForm());
    
    byte[] hashOfCrum = digest.digest();

    Builder builder = new FixedLeafBuilder(Digests.SHA_256.hashAlgo());
    
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
    CrumTrail trail = new CrumTrail(proof, crum);
    assertTrue(trail.verifyTrail(digest));
    
    Statement stmt = trail.toStatement();
    System.out.println();
    System.out.println(method(new Object() {  }) + ":");
    System.out.println(stmt);
    
    assertTrue(stmt.eval());
  }

}
