/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import java.nio.ByteBuffer;

/**
 * Main abstraction for witnessing hashes and vending out
 * receipts.
 */
public interface NotaryService {


  /** Returns static information about the service. */
  NotaryPolicy policy();


  /**
   * Witnesses the given {@code hash} and returns a receipt.
   * 
   * @param hash 32-bytes
   */
  Receipt witness(ByteBuffer hash);



  /**
   * Returns an updated receipt for the given {@code crum}.
   * If the crum is not found in its approproriate block
   * (i.e. it's made up), or if the crum has expired from the
   * service's memory, then {@code witness(crum.hash())} is returned.
   * 
   * @see Receipt#crum()
   */
  Receipt update(Crum crum);




  /**
   * Returns a hash proof linking the last block to the genesis block.
   * 
   * @return {@code stateProof(true, 1L)}
   */
  default BlockProof stateProof() {
    return stateProof(true, 1L);
  }


/**
   * Returns a hash proof linking the last block to the
   * given {@code target} block no., then linking the target
   * block no. to the genesis block.
   * 
   * @return {@code stateProof(true, 1L)}
   */
  default BlockProof stateProof(long target) {
    return target == 1L ?
        stateProof(true, 1L) :
         stateProof(true, 1L, target);
  }


  /**
   * Returns a hash proof asserting how the given target block no.s are
   * linked.
   * 
   * @param hi        if {@code true}, then the latest block no. is included
   *                  as a target
   * @param blockNos  strictly ascending target block no.s
   * 
   * @return a hash proof linking the target block no.s in the chain
   */
  BlockProof stateProof(boolean hi, Long... blockNos);

}



