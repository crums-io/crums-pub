/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.util.mrkl.hashing;


import static io.crums.model.hashing.ExpressionSymbols.*;
import static io.crums.util.IntegralStrings.*;

import java.util.List;
import java.util.Objects;

import io.crums.model.hashing.Entity;
import io.crums.model.hashing.Parser;
//import io.crums.model.hashing.Entity;
import io.crums.model.hashing.Statement;
import io.crums.util.hash.Digests;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.Tree;
import io.crums.util.mrkl.index.AbstractNode;
import io.crums.util.mrkl.index.TreeIndex;


/**
 * Generates {@linkplain Statement}s derived from Merkle {@linkplain Proof}s.
 * 
 * <h3>Structure</h3>
 * <p>
 * The general structure of returned statements is as follows.
 * <ol>
 * <li><em>LHS</em>. A hex literal equal to the value of the root node in the Merkle tree.
 *     (A proof redundantly contains the root value of the tree.)</li>
 * <li><em>RHS</em>. Expresses a path of hash pointers from the proof-item, to the root of the
 *     Merkle tree. The proof-item may be used as is, or may be substituted with an expression
 *     that evaluates to the proof-item (typically the hash of another byte string).</li>
 * </ol>
 * </p><p>
 * This way, the <em>intent</em> of such a statement is more transparent to a human reader;
 * to validate the statement of course, a piece of software (such as provided in this library)
 * must be used. 
 * </p>
 */
public class ProofStatement {
  
  private ProofStatement() { }
  
  public final static String LEAF_PAD = toHex(Tree.LEAF_PAD);
  public final static String BRANCH_PAD = toHex(Tree.BRANCH_PAD);
  
  
  /**
   * Creates and returns a <tt>Statement</tt> for the given <tt>proof</tt>.
   */
  public static Statement createStatement(Proof proof) {
    return createItemStatement(proof, null);
  }
  
  
  /**
   * Creates and returns a <tt>Statement</tt> for the given proof and <em>optional</em>
   * item definition <tt>itemDef</tt>. If provided, this must {@linkplain Parser#parse(String) parse}
   * to an {@linkplain Entity entity} that evaluates to the value of the {@linkplain Proof#item() item}
   * in the proof; if not provided (<tt>null</tt>), then a hex literal equal to the proof's item itself
   * is used.
   */
  public static Statement createItemStatement(Proof proof, CharSequence itemDef) {
    Objects.requireNonNull(proof, "null proof");
    if (!Digests.SHA_256.hashAlgo().equals(proof.getHashAlgo()))
      throw new IllegalArgumentException("proof hash algo: " + proof.getHashAlgo());
    if (itemDef == null)
      itemDef = toHex(proof.item());
    return generateStatement(proof, itemDef);
  }
  
  
  
  
  
  
  
  
  

  private static Statement generateStatement(Proof proof, CharSequence itemDef) {
    
    if (itemDef == null)
      itemDef = toHex(proof.item());
    
    try {
      List<byte[]> chain = proof.hashChain();
      
      TreeIndex<?> tree = TreeIndex.newGeneric(proof.leafCount());
      
      StringBuilder expr = new StringBuilder(70 * chain.size());
      expr.append(itemDef).append(' ').append(FLIP).append(LEAF_PAD);
      
      AbstractNode node = tree.getSibling(0, proof.leafIndex());
      
      String sibHex = toHex(chain.get(1));
      
      if (node.isLeaf()) {
        if (node.isRight()) {
          expr.append(' ').append(LEAF_PAD).append(' ').append(sibHex);
        } else {
          group(expr).append(' ').append(FLIP).append(' ')
          .append(LFT_BRKT).append(LEAF_PAD).append(' ').append(sibHex).append(RGT_BRKT);
        }
      } else {
        assert node.isLeft();
        group(expr).append(' ').append(FLIP).append(' ')
        .append(LFT_BRKT).append(BRANCH_PAD).append(' ').append(sibHex).append(RGT_BRKT);
      }
      
      
      node = tree.getParent(node);
      hash(expr);
      
      int cindex = 2;
      
      for (; node.level() != tree.height(); node = tree.getParent(node), ++cindex) {
        // invariant: *expr*, if evaluated, is the value of *node*;
        // the values in the chain are the *siblings of each computed parent
        sibHex = toHex(chain.get(cindex));
        
        expr.append(FLIP).append(BRANCH_PAD).append(' ');
        
        AbstractNode sib = tree.getSibling(node);
        String sibPadding = sib.isLeaf() ? LEAF_PAD : BRANCH_PAD;
        
        if (sib.isLeft()) {
          expr.append(FLIP).append(LFT_BRKT).append(sibPadding).append(' ')
          .append(sibHex).append(RGT_BRKT);
        } else {
          expr.append(sibPadding).append(' ').append(sibHex);
        }
        
        hash(expr);
      }
      
      
      if (cindex != chain.size() - 1)
        throw new IllegalArgumentException(
            "proof hash chain is *structurally* invalid (too long): expected " + (cindex + 1) +
            " elements; actual given is " + chain.size());

      
//      expr.append(' ').append(EQU).append(' ').append(toHex(proof.rootHash()));
      
      // Group all values of interest to a human at the start of the expresssion
      return Statement.parse(toHex(proof.rootHash()) + " " + EQU + " " + expr);
    
    } catch (IndexOutOfBoundsException chainTooShort) {
      throw new IllegalArgumentException(
          "proof hash chain is *structurally* invalid (too short)", chainTooShort);
    }
  }
  
  
  
  
  private static StringBuilder hash(StringBuilder expression) {
    return expression.insert(0, LFT_PRNS).append(RGT_PRNS);
  }
  
  private static StringBuilder group(StringBuilder expression) {
    return expression.insert(0, LFT_BRKT).append(RGT_BRKT);
  }

}
