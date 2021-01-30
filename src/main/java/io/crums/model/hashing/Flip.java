/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import static io.crums.model.hashing.ExpressionSymbols.*;


import java.nio.ByteBuffer;
import java.util.Objects;


/**
 * The <tt>xxx : xxx</tt> entity defined by the result of a flip (transposition).
 */
final class Flip extends BaseEntity {
  
  
  
  private final Entity first;
  private Entity second;
  
  
  public Flip(Entity first) {
    this(first, null);
  }
  
  public Flip(Entity first, Entity second) {
    this.first = Objects.requireNonNull(first, "null first");
    this.second = second;
  }


  @Override
  public ByteBuffer bytes() {
    ByteBuffer firstBytes = first.bytes();
    ByteBuffer secondBytes = second().bytes();
    ByteBuffer out = ByteBuffer.allocate(firstBytes.remaining() + secondBytes.remaining());
    return out.put(secondBytes).put(firstBytes).flip().asReadOnlyBuffer();
  }
  
  
  
  @Override
  public CharSequence toText() {
    
    CharSequence a = first.toText();
    CharSequence b = second().toText();
    
    return
        new StringBuilder(a.length() + b.length() + 3)
        .append(a).append(' ').append(FLIP).append(' ').append(b);
  }


  @Override
  public boolean delimited() {
    return false;
  }
  
  
  
  public void setArg(Entity entity) {
    this.second = entity;
  }
  
  
  Entity second() {
    Entity e = second;
    if (e == null)
      throw new IllegalStateException("second entity not initialized");
    return e;
  }
  

  @Override
  public boolean isComplete() {
    return second != null;
  }
  
  
  @Override
  public boolean isFlip() {
    return true;
  }
  
  
  
//  @Override
//  public boolean equals(Entity entity) {
//    return
//        entity instanceof Flip ?
//            equals((Flip) entity) :
//              super.equals(entity);
//  }
//
//
//  /**
//   * A more efficient implementation of {@linkplain #equals(Entity)}.
//   * 
//   * @param other non-null
//   */
//  public boolean equals(Flip other) {
//    return
//        other == this ||
//        flip == other.flip &&
//        other.first.equals(first) &&
//        other.second.equals(second);
//  }

}

















