/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Two entities separated by an '=' sign.
 * 
 * @see Parser
 */
public class Statement {
  
  private static class FirstReader extends FilterReader {
    
    private boolean end;

    private FirstReader(Reader in) {
      super(in);
    }

    @Override
    public int read() throws IOException {
      if (end)
        return -1;
      
      int out = super.read();
      if (out == ExpressionSymbols.EQU) {
        end = true;
        return -1;
      }
      return out;
    }
    
    boolean hitEquals() {
      return end;
    }
    
  }
  
  
  
  /**
   * Parses and returns a statement. The argument must include exactly 1 (one)
   * equal sign '='.
   */
  public static Statement parse(String statement) {
    return parse(new StringReader(statement));
  }
  

  /**
   * Parses and returns a statement. The stream must include exactly 1 (one)
   * equal sign '='. Note the stream <em>must</em> end at the end of the
   * RHS (it is not a self-delimiting structure).
   */
  public static Statement parse(Reader reader) throws UncheckedIOException {
    Objects.requireNonNull(reader, "null reader");
    
    FirstReader leftReader = new FirstReader(reader);
    Entity lhs = Parser.parse(leftReader);
    if (!leftReader.hitEquals())
      throw new HashingException("missing '" + ExpressionSymbols.EQU + "': " + lhs);
    
    Entity rhs = Parser.parse(reader);
    
    return new Statement(lhs, rhs);
  }
  
  
  
  
  
  
  private final Entity left;
  private final Entity right;
  
  public Statement(Entity left, Entity right) {
    this.left = Objects.requireNonNull(left, "null left");
    this.right = Objects.requireNonNull(right, "null right");
  }
  
  
  /**
   * Returns the LHS of the statement.
   */
  public Entity left() {
    return left;
  }
  
  /**
   * Returns the RHS of the statement.
   */
  public Entity right() {
    return right;
  }
  
  
  /**
   * Evaluates the left- and righthand side and returns whether they evaluated to the same
   * bytes sequence.
   * 
   * @return {@code left().bytes().equals(right().bytes())}
   * 
   * @see Entity#bytes()
   */
  public boolean eval() {
    return left.bytes().equals(right.bytes());
  }
  
  
  public CharSequence toText() {
    CharSequence lhs = left.toText();
    CharSequence rhs = right.toText();
    StringBuilder s = new StringBuilder(lhs.length() + rhs.length() + 1);
    return s.append(lhs).append(ExpressionSymbols.EQU).append(rhs);
  }
  
  
  @Override
  public String toString() {
    return toText().toString();
  }

}












