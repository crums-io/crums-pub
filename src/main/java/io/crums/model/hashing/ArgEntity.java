/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;

import java.nio.ByteBuffer;

/**
 * Bound entity. "[ ]" or "( )".
 */
abstract class ArgEntity extends BaseEntity {

  
  private Entity source;

  ArgEntity() {  }
  
  ArgEntity(Entity source) {
    this.source = source;
  }


  /**
   * @return <tt>true</tt>
   */
  @Override
  public final boolean delimited() {
    return true;
  }

  @Override
  public ByteBuffer bytes() {
    return arg().bytes();
  }
  
  
  @Override
  public final CharSequence toText() {
    CharSequence argText = arg().toText();
    return
        new StringBuilder(argText.length() + 2)
        .append(openChar())
        .append(argText)
        .append(closeChar());
  }
  
  /**
   * Parenthesizing opening char.
   */
  abstract char openChar();
  /**
   * Parenthesizing closing char.
   */
  abstract char closeChar();
  

  
  /**
   * Sets the argument. The instance's state is invalid prior to
   * invocation with non-null value.
   * 
   * @see #arg()
   */
  @Override
  public void setArg(Entity source) {
    this.source = source;
  }
  

  /**
   * Returns the argument.
   * 
   * @throws IllegalStateException if arg not set
   * @see #setArg(Entity)
   */
  protected final Entity arg() throws IllegalStateException {
    Entity e = source;
    if (e == null)
      throw new IllegalStateException("argument not set");
    return e;
  }
  

  @Override
  public final boolean isComplete() {
    return source != null;
  }

}


