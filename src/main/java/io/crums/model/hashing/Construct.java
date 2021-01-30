/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Objects;

/**
 * A well-formed expression. {@linkplain Parser}'s public implementation of the
 * {@linkplain Entity} interface.
 */
public final class Construct implements Entity {
  
  private final Entity entity;
  
  private final String text;
  
  
  Construct(Entity entity, CharSequence text) {
    this.entity = Objects.requireNonNull(entity, "null entity");
    this.text = Objects.requireNonNull(text, "null text").toString();
  }


  @Override
  public boolean delimited() {
    return entity.delimited();
  }


  @Override
  public ByteBuffer bytes() {
    return entity.bytes();
  }


  /**
   * {@inheritDoc}
   * This is just the string the parser read. I.e. formatting is preserved.
   */
  @Override
  public String toText() {
    return text;
  }
  
  /**
   * Returns {@linkplain #toText()} in default whitespace formatting.
   * 
   * @return a <em>single</em> line
   */
  public String canonicalText() {
    return entity.toText().toString().toLowerCase(Locale.ROOT);
  }
  
  
  /**
   * Returns {@linkplain #toText()}.
   */
  public String toString() {
    return toText().toString();
  }

}
