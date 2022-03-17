/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;


import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.model.Constants;
import io.crums.util.mrkl.Builder;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.Tree;

/**
 * 
 */
public class ProofParserTest {
  
  
  
  
  
  


  @Test
  public void testRoundtrip() throws Exception {
    int leafCount = 0x55; // 85
    testRoundtripImpl(leafCount);
  }
  


  
  private void testRoundtripImpl(int leafCount) throws Exception {
    
    Random rand = new Random(leafCount);
    
    Builder treeBuilder = new Builder(Constants.HASH_ALGO);
    byte[][] leaves = new byte[leafCount][];
    for (int index = 0; index < leafCount; ++index) {
      byte[] hash = new byte[Constants.HASH_WIDTH];
      rand.nextBytes(hash);
      treeBuilder.add(hash);
      leaves[index] = hash;
    }
    Tree tree = treeBuilder.build();
    byte[] root = tree.root().data();
    
    
    ProofParser parser = new ProofParser();
    

    // try the right edge first (this typically has the most corner cases)
    // but note: we're not testing the tree structure here (tested elsewhere)
    
    MessageDigest digest = MessageDigest.getInstance(Constants.HASH_ALGO);
    
    for (int leafIndex = leafCount - 1; leafIndex-- > 0; ) {
      Proof expected = new Proof(tree, leafIndex);
      String proofText = parser.toJsonObject(expected).toString();
      
//      System.out.println(proofText);
//      System.out.println();
      
      Proof read = parser.toProof(proofText);
      assertEquals(expected, read);
      assertTrue(read.verify(digest));
      assertArrayEquals(leaves[leafIndex], read.item());
      assertArrayEquals(root, read.rootHash());
    }
    
  }

}
