/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import static io.crums.model.hashing.ExpressionSymbols.*;

/**
 * "[ ]" type.
 */
final class Bracketed extends ArgEntity {

  
  
  /**
   * Creates an empty <em>modifiable</em> instance.
   * Used by the {@linkplain Parser parser}.
   */
  Bracketed() {  }
  
  /**
   * Creates a read-only instance. The list is copied.
   */
  public Bracketed(Entity arg) {
    super(arg);
  }
  
  
  
  

  @Override
  char openChar() {
    return LFT_BRKT;
  }

  @Override
  char closeChar() {
    return RGT_BRKT;
  }

}









