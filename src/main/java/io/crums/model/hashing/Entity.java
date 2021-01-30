/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Every object in our model is an entity.
 */
public interface Entity {
  
  
  /**
   * Determines if the text representation of the instance is self-delimiting.
   * This figures when deciding whether to bracket (group) entities in, for
   * example, a binary operation.
   */
  boolean delimited();
  
  
  
  /**
   * Returns the entity's state as a string of bytes.
   * Depending on the entity type, this may involve a computation.
   * 
   * @return a <em>read-only</em> view of the instance's bytes.
   */
  ByteBuffer bytes();
  
  /**
   * Returns a parseable representation of the entity.
   */
  CharSequence toText();
  
  
  /**
   * <p>Equality semantics is governed by {@linkplain #bytes()}. Implementations
   * override to exploit logical shortcuts if computing {@linkplain #bytes()}
   * is expensive.
   * </p>
   * <h3>Independence From Object::equals</h3>
   * <p>
   * <em>Note this method is generally
   * <b>not consistent</b> with <tt>Object.equals(Object)</tt></em> (and therefore
   * not consistent with {@linkplain #hashCode()} either).
   * </p>
   * 
   * @param entity non-null
   */
  default boolean equals(Entity entity) {
    return entity == this || Objects.requireNonNull(entity, "null entity").bytes().equals(bytes());
  }
}













