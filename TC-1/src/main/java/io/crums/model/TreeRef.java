/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


import java.nio.ByteBuffer;

/**
 * A stub to a crum Merkle tree identified by its root hash. Additional info provided
 * by this stub type include the {@linkplain #treeNumber() tree number},
 * the tree's {@linkplain #minUtc() minimum} and {@linkplain #maxUtc() maximum} UTCs.
 */
public class TreeRef extends HashUtc implements Comparable<TreeRef> {
  
  public final static int TREE_REF_SIZE = HashUtc.DATA_SIZE + 8;


  /**
   * Writes the specified fields to the given <code>out</code> buffer. The buffer's position is advanced by 
   * {@linkplain #TREE_REF_SIZE}-many bytes.
   * 
   * @param hash      {@linkplain Constants#HASH_WIDTH} bytes long
   * @param firstUtc  the min utc in the tree
   * @param lastUtc   the max utc in the tree
   * @param out the output buffer
   * @return <code>out</code>
   */
  public static ByteBuffer writeTreeRefToBuffer(byte[] hash, long firstUtc, long lastUtc, ByteBuffer out) {
    return writeToBuffer(hash, firstUtc, out).putLong(lastUtc);
  }
  
  
  /**
   * Writes the specified fields to the given <code>out</code> buffer. The buffer's position is advanced by 
   * {@linkplain #TREE_REF_SIZE}-many bytes.
   * 
   * @param hash      {@linkplain Constants#HASH_WIDTH} remaining bytes
   * @param firstUtc  the min utc in the tree
   * @param lastUtc   the max utc in the tree
   * @param out the output buffer
   * @return <code>out</code>
   */
  public static ByteBuffer writeTreeRefToBuffer(ByteBuffer hash, long firstUtc, long lastUtc, ByteBuffer out) {
    return writeToBuffer(hash, firstUtc, out).putLong(lastUtc);
  }
  
  
  
  
  
  
  
  
  
  private final int treeNumber;

  /**
   * Creates a new instance.
   * 
   * @param data at least {@linkplain #TREE_REF_SIZE} remaining bytes. The caller agrees not to modify in any way.
   */
  public TreeRef(ByteBuffer data, int treeNumber) {
    super(data);
    this.treeNumber = treeNumber;
    
    if (data.remaining() < TREE_REF_SIZE)
      throw new IllegalArgumentException("data.remaining() " + data.remaining());
    long lastUtc = maxUtc();
    if (lastUtc < minUtc())
      throw new IllegalArgumentException("lastUtc() " + lastUtc + " < firstUtc() " + minUtc());
    sanityCheckUtc(lastUtc);

    if (treeNumber < 0)
      throw new IllegalArgumentException("treeNumber " + treeNumber);
  }
  
  
  

  
  
  /**
   * Returns the zero-based tree number.
   */
  public int treeNumber() {
    return treeNumber;
  }
  
  
  /**
   * Returns the minimum crum utc in the tree. Synonym for {@link #utc()}.
   */
  public long minUtc() {
    return utc();
  }

  /**
   * Returns the maximum crum utc in the tree.
   */
  public long maxUtc() {
    return data.getLong(DATA_SIZE);
  }

  /**
   * Instances are ordered firstly by {@linkplain #utc() utc}, secondly by {@linkplain #hash() hash}.
   * Consistent with {@linkplain #equals(Object)}.
   */
  @Override
  public int compareTo(TreeRef o) {
    int comp = Long.compare(utc(), o.utc());
    return comp == 0 ? HASH_COMPARATOR.compare(this, o) : comp;
  }

  /**
   * Returns the instance's serialized representation as a read-only buffer.
   * Note an instance's serial form does not include the {@linkplain #treeNumber() treeNumber}.
   * 
   * @return a buffer with {@linkplain #DATA_SIZE} remaining bytes.
   * 
   * @see #TreeRef(ByteBuffer, int)
   */
  @Override
  public ByteBuffer serialForm() {
    return data.asReadOnlyBuffer().limit(TREE_REF_SIZE);
  }

}
