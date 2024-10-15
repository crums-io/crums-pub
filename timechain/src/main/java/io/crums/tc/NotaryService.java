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
   * @param hash          32-bytes remaining
   * @param fromBlockNo   positive block no. block proof in crumtrail begins at
   *                      (very rarely matters, since this method is typically
   *                      returns a receipt with only a crum and must be
   *                      followed by {@linkplain #update(Crum, long)})
   * 
   * @return a receipt containing either a crum, or in rare cases, a crumtrail
   */
  Receipt witness(ByteBuffer hash, long fromBlockNo);


  /**
   * Witnesses the given {@code hash} and returns a receipt.
   * 
   * @return {@code witness(hash, 1L)}
   * @see #witness(ByteBuffer, long)
   */
  default Receipt witness(ByteBuffer hash) {
    return witness(hash, 1L);
  }


  /**
   * Returns an updated receipt for the given {@code crum}.
   * If the crum is not found in its approproriate block
   * (i.e. it's made up), or if the crum has expired from the
   * service's memory, then {@code witness(crum.hash(), fromBlockNo)}
   * is returned.
   * 
   * @param crum          a crum from a previous receipt
   * @param fromBlockNo   starting block no. in crumtrail's block proof
   * 
   * @see Receipt#crum()
   */
  Receipt update(Crum crum, long fromBlockNo);


  /**
   * Returns an updated receipt for the given {@code crum}.
   * 
   * @return {@code update(crum, 1L)}
   * @see #update(Crum, long)
   */
  default Receipt update(Crum crum) {
    return update(crum, 1L);
  }




  /**
   * Returns a hash proof linking the last block to the genesis block.
   * 
   * @return {@code stateProof(true, 1L)}
   * @see #stateProof(boolean, Long...)
   */
  default BlockProof stateProof() {
    return stateProof(true, 1L);
  }


/**
   * Returns a hash proof linking the last block to the
   * given {@code target} block no., then linking the target
   * block no. to the genesis block.
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



