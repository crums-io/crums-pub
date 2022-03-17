/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.util.mrkl.hashing;


import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gnahraf.test.SelfAwareTestCase;

import io.crums.model.hashing.Statement;
import io.crums.util.hash.Digests;
import io.crums.util.mrkl.FixedLeafBuilder;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.Tree;

/**
 * 
 */
public class ProofStatementTest extends SelfAwareTestCase {
  
  
  @Test
  public void testOne() {
    Object label = new Object() { };
    int count = 2_000_000;
    int index = 1_500_020;
  
    
    Tree tree = newRandomTree(count);
    Proof proof = new Proof(tree, index);
    assertTrue(proof.verify(Digests.SHA_256.newDigest()));
    Statement pstmt = ProofStatement.createStatement(proof);
    System.out.println(method(label) + ":");
    System.out.println(pstmt);
//    System.out.println("LHS: " + pstmt.statement().left().bytesHex());
//    System.out.println("RHS: " + pstmt.statement().right().bytesHex());
    long nanos = System.nanoTime();
    assertTrue(pstmt.eval());
    long lap = System.nanoTime() - nanos;
    System.out.println(
        "Evaluation took " + lap + " nano secs (" + ((lap /1000) * 1e-3) + " millis)");
    System.out.println();
  }
  

  @Test
  public void testMany() {
    Object label = new Object() { };
    final int count = 2_000_001;
    
    final int sampleSize = 1000;
    Tree tree = newRandomTree(count);
    Random random = new Random(99);
    
    long evalNanos = 0;
    
    for (int countdown = sampleSize; countdown-- > 0; ) {
      int index = random.nextInt(count);
      evalNanos += testInstance(tree, index);
    }
    
    long avgEvalNano = evalNanos / sampleSize;
    
    System.out.println(method(label) + ":");
    System.out.println("count: " + count);
    System.out.println("samples: " + sampleSize);
    System.out.println("avg eval time: " + ((avgEvalNano /1000) * 1e-3) + " milliseconds");
    System.out.println();
  }
  

  @Test
  public void testManyAtEdge() {
    Object label = new Object() { };
    final int count = 2_000_001;
    
    final int sampleSize = 1000;
    Tree tree = newRandomTree(count);
    
    long evalNanos = 0;
    
    for (int index = count - sampleSize; index < count; ++index) {
      evalNanos += testInstance(tree, index);
    }
    
    long avgEvalNano = evalNanos / sampleSize;
    
    System.out.println(method(label) + ":");
    System.out.println("count: " + count);
    System.out.println("samples: " + sampleSize);
    System.out.println("avg eval time: " + ((avgEvalNano /1000) * 1e-3) + " milliseconds");
    System.out.println();
  }
  
  
  private long testInstance(Tree tree, int index) {
    Proof proof = new Proof(tree, index);
    assertTrue(proof.verify(Digests.SHA_256.newDigest()));
    Statement pstmt = ProofStatement.createStatement(proof);
    long nanos = System.nanoTime();
    boolean eval = pstmt.eval();
    long lap = System.nanoTime() - nanos;
    assertTrue(eval);
    return lap;
  }
  
  
  
  
  
  
  public static Tree newRandomTree(int leafCount) {
    var builder = new FixedLeafBuilder(Digests.SHA_256.hashAlgo());
    Random random = new Random(leafCount);
    byte[] leafEntry = new byte[Digests.SHA_256.hashWidth()];
    for (int index = 0; index < leafCount; ++index) {
      random.nextBytes(leafEntry);
      builder.add(leafEntry);
    }
    return builder.build();
  }

}
