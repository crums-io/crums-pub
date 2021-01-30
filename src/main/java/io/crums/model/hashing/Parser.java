/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import static io.crums.model.hashing.ExpressionSymbols.*;
import static io.crums.util.Strings.*;
import static io.crums.util.IntegralStrings.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import io.crums.util.Lists;

/**
 * Parser for the grammer. This is a super-simple grammer, so we do away with a
 * formal specification.
 * 
 * <h3>Grammer</h3>
 * <p>
 *  Each object represents a final sequence of bytes. There are 4 types object:
 *  <ol>
 *  <li><em>Literal</em>. An even sequence of hexadecimal digits, each pair specifying a byte
 *  value.</li>
 *  <li><em>Grouped</em> Symbols <code>[</code> <code>]</code>. An object whose bytes are the
 *  concatentation
 *  of the bytes of a non-empty sequence of objects. The object sequence is bounded by brackets .</li>
 *  <li><em>Flipped</em> Symbol <code>:</code>. A pair of adjacent objects separated by a colon
 *  and by the rules of our grammer, transposed in position.</li>
 *  <li><em>Hashed</em>. Symbols <code>(</code> <code>)</code>. An object whose bytes are the
 *  <em>hash</em> of a concatenated sequence of objects bound by parentheses.</li>
 *  </ol>
 *  Whitespace is the usual neutral delimiter.
 * </p>
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Shunting-yard_algorithm">Shunting-yard algorithm</a>
 */
public class Parser {
  
  
  /**
   * Parses the given snippet and returns it as a {@linkplain Construct}.
   */
  public static Construct parse(String snippet) {
    return parse(new StringReader(snippet));
  }
  
  
  /**
   * Parsers the given stream and return the result as a {@linkplain Construct}. The
   * stream is read to the end.
   */
  public static Construct parse(Reader reader) throws UncheckedIOException {
    try{
      return new Parser().parseImpl(reader);
    } catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
  
  
  /**
   * Operands stack.
   */
  private final Deque<BaseEntity> operands = new ArrayDeque<>();
  /**
   * For simpler detection of unmatched bounds '[' and '('.
   */
  private final Deque<Character> boundsStack = new ArrayDeque<>();
  /**
   * What's been read so far.
   */
  private final StringBuilder tally = new StringBuilder(2048);
  
  
  private int flipsPending;
  
  
  private Parser() {  }
  
  
  
  
  private Construct parseImpl(Reader reader) throws IOException {
    
    StringBuilder hexBuffer = new StringBuilder(512);
    while (true) {
      char c;
      {
        int i = reader.read();
        if (i == -1) {
          fireHexString(hexBuffer);
          break;
        }
        c = (char) i;
      }

      tally.append(c);
      
      
      if (isHexDigit(c)) {
        hexBuffer.append(c);
      } else if (isWhitespace(c)) {
        fireHexString(hexBuffer);
      } else if (c == LFT_BRKT) {
        fireHexString(hexBuffer);
        fireBracketStart();
      } else if (c == RGT_BRKT) {
        fireHexString(hexBuffer);
        fireBracketEnd();
      } else if (c == LFT_PRNS) {
        fireHexString(hexBuffer);
        fireHashStart();
      } else if (c == RGT_PRNS) {
        fireHexString(hexBuffer);
        fireHashEnd();
      } else if (c == FLIP) {
        fireHexString(hexBuffer);
        fireFlip();
      } else {
        throw makeParseError("illegal char '" + c + "'").fillInStackTrace();
      }
      
    }
    
    if (operands.isEmpty())
      throw makeParseError("no symbols to parse").fillInStackTrace();
    
    if (!boundsStack.isEmpty())
      throw makeParseError("incomplete statement, '" + boundsStack.pop() + "' not closed").fillInStackTrace();
    
    if (!operands.peek().isComplete())
      throw makeParseError("incomplete statement, missing operand for '" + FLIP + "'").fillInStackTrace();
    
    int count = operands.size();
    switch (count) {
    case 0:
      throw new IllegalArgumentException("no symbols to parse");
    case 1:
      return new Construct(operands.pop(), tally);
    }
    
    ArrayList<Entity> objects = new ArrayList<>(count);
    for (Iterator<BaseEntity> i = operands.descendingIterator(); i.hasNext();)
      objects.add(i.next());
    
    return new Construct(new Concat(objects), tally);
  }
  
  
  
  


  

  private void fireHexString(StringBuilder hexBuffer) {
    int hexSize = hexBuffer.length();
    if (hexSize != 0) {
      fireHexString(hexBuffer.toString());
      hexBuffer.setLength(0);
    }
  }

  private void fireHexString(String hex) {
    
    ByteString bstr;
    
    try {
      bstr = new ByteString(hex);
    } catch (Exception x) {
      String preamble =
          "failed to parse byte literals '" + hex + "' (" + x.getMessage() + ")";
      throw makeParseError(preamble).fillInStackTrace();
    }
    
    pushCompleted(bstr);
  }



  private final static Character O_BRKT = LFT_BRKT;
  private final static Character C_BRKT = RGT_BRKT;

  private void fireBracketStart() {
    operands.push(new Bracketed());
    boundsStack.push(O_BRKT);
  }
  
  
  
  
  private void fireBracketEnd() {
    assignArg(O_BRKT, C_BRKT);
  }
  

  private final static Character O_HASH = LFT_PRNS;
  private final static Character C_HASH = RGT_PRNS;
  
  private void fireHashStart() {
    operands.push(new Hashed());
    boundsStack.push(O_HASH);
  }
  
  
  // see fireBracketEnd for comments
  private void fireHashEnd() {
    assignArg(O_HASH, C_HASH);
  }

  
  
  private void fireFlip() {
    if (operands.isEmpty() || !operands.peek().isComplete())
      throw makeParseError("binary op '" + FLIP + "' must follow an operand").fillInStackTrace();
    
    BaseEntity prev = operands.pop();
    operands.push(new Flip(prev));
    ++flipsPending;
  }
  
  
  private void assignArg(Character open, Character close) {
    
    if (boundsStack.isEmpty() || boundsStack.pop() != open)
      throw makeParseError("unmatched '" + close + "'");
    
    // pop to argument -- see for eg fireHashStart()
    // and collect elements, along the way
    BaseEntity argEntity = null;
    List<Entity> elements = new ArrayList<>(8);
    
    while (argEntity == null) {
      BaseEntity e = operands.pop();
      if (e.isComplete())
        elements.add(e);
      else
        argEntity = e;
    }
    
    assert open.equals(O_HASH) ? argEntity instanceof Hashed : argEntity instanceof Bracketed;
    
    if (elements.isEmpty())
      throw makeParseError("missing operand").fillInStackTrace();
    
    // (elements were added in reverse)
    elements = Lists.reverse(elements);
    
    argEntity.setArg(Concat.stitch(elements));
    
    // replace the argEntity with a delegate, so the next
    // invocation of this method doesn't pick it up.
    pushCompleted(argEntity);
  }
  


  
  
  

  
  // special handling for binary ops
  // (of which there is only 1 now: flip)
  private void pushCompleted(BaseEntity entity) {
    
    assert flipsPending >= 0;
    
    if (flipsPending == 0) {
      operands.push(entity);
      return;
    }
    
    BaseEntity prev = operands.peek();
    
    if (prev != null && prev.isFlip() && !prev.isComplete()) {
      
      operands.pop();
      prev.setArg(entity);
      --flipsPending;
      pushCompleted(prev);
    
    } else {
      operands.push(entity);
    }
  }




  private HashingException makeParseError(String preamble) {

    int offset = tally.length() - 1;
    
    String snippet;
    if (offset <= 16)
      snippet = tally.toString();
    else
      snippet = "..." + tally.subSequence(offset - 13, tally.length());
    
    return new HashingException(preamble + ": " + snippet + "<-- (offset " + offset + ")");
  }

}
