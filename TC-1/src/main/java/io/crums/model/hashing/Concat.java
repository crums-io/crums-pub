/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import java.nio.ByteBuffer;
//import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Concatenation of a list of entities, left-to-right.
 */
final class Concat extends BaseEntity {
  
  
  /**
   * Returns the list of entities and as a single entity, which in most
   * cases (given a list of 2 or more) is an instance of this class.
   */
  public static Entity stitch(List<Entity> elements) {
    int size = Objects.requireNonNull(elements, "null elements").size();
    switch (size) {
    case 0:
      throw new IllegalArgumentException("empty elements");
    case 1:
      return elements.get(0);
    }
    return new Concat(elements);
  }
  
  
  
  
  
  
  
  
  private List<Entity> elements;
  
  
  
  public Concat() {  }
  
  public Concat(List<Entity> elements) {
    this.elements = elements;
  }
  
  


  @Override
  public boolean isComplete() {
    return elements != null && !elements.isEmpty();
  }
  
  

  @Override
  public boolean delimited() {
    return elements().size() == 1;
  }

  @Override
  public ByteBuffer bytes() {
    List<Entity> entities = elements();
    int size = entities.size();
    
    ByteBuffer[] bb = new ByteBuffer[size];
    int bytes = 0;
    for (int index = 0; index < size; ++index) {
      ByteBuffer b = entities.get(index).bytes();
      bytes += b.remaining();
      bb[index] = b;
    }
    
    ByteBuffer out = ByteBuffer.allocate(bytes);
    for (int index = 0; index < size; ++index)
      out.put(bb[index]);
    
    return out.flip();
  }
  
  
  

  @Override
  public CharSequence toText() {
    List<Entity> entities = elements();
    int size = entities.size();
    
    CharSequence[] texts = new CharSequence[size];
    int chars = 0;
    for (int index = 0; index < size; ++index) {
      CharSequence t = entities.get(index).toText();
      chars += t.length();
      texts[index] = t;
    }
    
    StringBuilder out = new StringBuilder(chars + size - 1).append(texts[0]);
    for (int index = 1; index < size; ++index)
      out.append(' ').append(texts[index]);
    
    return out;
  }
  
  
  List<Entity> elements() {
    List<Entity> list = elements;
    if (list == null || list.isEmpty())
      throw new IllegalStateException("elements not initialized: " + list);
    return list;
  }

}
