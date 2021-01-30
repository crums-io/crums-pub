/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


/**
 * Parser abstract base type for entities. Entities are pushed onto the parser's
 * <em>operand stack</em>. Some of the entities it places on this stack are
 * initially incomplete: they are completed as more characters are read in.
 */
abstract class BaseEntity implements Entity {

  
  
  /**
   * Determines if the parser completed this entity.
   */
  public abstract boolean isComplete();
  
  /**
   * Sets the argument for the entity. A noop for most entity types.
   */
  public void setArg(Entity entity) {  }
  
  
  /**
   * Determines if this entity is the result of the flip <em>binary</em>
   * operation ":". (If we had more than one such binary operation, our design
   * would necessitate we return an enum prolly.)
   */
  public boolean isFlip() {
    return false;
  }
  
  /**
   * Returns {@linkplain #toText()}<tt>.toString()</tt>.
   */
  @Override
  public String toString() {
    return toText().toString();
  }

}
