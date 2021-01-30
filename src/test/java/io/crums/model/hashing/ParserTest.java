/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import static io.crums.util.IntegralStrings.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import com.gnahraf.test.SelfAwareTestCase;

import io.crums.util.hash.Digest;
import io.crums.util.hash.Digests;

import org.junit.Test;

/**
 * 
 */
public class ParserTest extends SelfAwareTestCase {
  
  final static Digest DIGEST = Digests.SHA_256;
  
  
  @Test
  public void testEmpty() {
    Object label = new Object() { };
    String input = "   \f \n  \r\f  \t";
    
    testMalformed(input, label);
  }
  
  
  @Test
  public void testLiteral() {
    Object label = new Object() { };
    String input = "e9";
    boolean delimited = true;
    
    testExpression(input, input, delimited, label);
  }
  
  
  @Test
  public void testLiteral2() {
    Object label = new Object() { };
    String input = "01 77";
    String expectedHex = "0177";
    boolean delimited = false;

    testExpression(input, expectedHex, delimited, label);
  }
  
  
  @Test
  public void testMalformedLiteral() {
    Object label = new Object() { };
    String input = "010";
    
    testMalformed(input, label);
  }
  
  
  @Test
  public void testLiteral3() {
    Object label = new Object() { };
    String input = "5fc3  77 48";
    String expectedHex = "5fc37748";
    boolean delimited = false;

    testExpression(input, expectedHex, delimited, label);
  }
  
  
  @Test
  public void testGroup() {
    Object label = new Object() { };
    String input = "[bd]";
    String expectedHex = "bd";
    boolean delimited = true;
    
    testExpression(input, expectedHex, delimited, label);
  }
  
  
  @Test
  public void testGroup2() {
    Object label = new Object() { };
    String input = "[bd ab52]";
    String expectedHex = "bdab52";
    boolean delimited = true;
    
    testExpression(input, expectedHex, delimited, label);
  }
  
  
  @Test
  public void testMalformedGroup() {
    Object label = new Object() { };
    String input = "[bd ab5]";

    testMalformed(input, label);
  }
  
  
  @Test
  public void testGroup3() {
    Object label = new Object() { };
    String input = "[ bd  ab52 10fe ]";
    String expectedHex = "bdab5210fe";
    boolean delimited = true;
    
    testExpression(input, expectedHex, delimited, label);
  }
  
  
  @Test
  public void testNestedGroup() {
    Object label = new Object() { };
    String input = "[ bd  [ab52] 10fe ]";
    String expectedHex = "bdab5210fe";
    boolean delimited = true;
    
    testExpression(input, expectedHex, delimited, label);
  }
  
  
  @Test
  public void testHash() {
    Object label = new Object() { };
    String inputSansHash = "ab52";
    String input = '(' + inputSansHash + ")";
    ByteBuffer expected = hash(inputSansHash);
    boolean delimited = true;
    
    testExpression(input, expected, delimited, label);
  }
  
  
  @Test
  public void testHash2() {
    Object label = new Object() { };
    
    String inputSansHash = "ab : 52";
    
    String input = '(' + inputSansHash + ")";
    ByteBuffer expected = hash("52ab");
    boolean delimited = true;
    
    testExpression(input, expected, delimited, label);
  }
  
  
  @Test
  public void testHash3() {
    Object label = new Object() { };
    
    String inputSansHash = "[bb:99] :[[cd 33]:ee] 44";
    
    String input = '(' + inputSansHash + ")";
    ByteBuffer expected = hash("eecd3399bb44");
    boolean delimited = true;
    
    testExpression(input, expected, delimited, label);
  }
  
  
  @Test
  public void testHash4() {
    Object label = new Object() { };

    String input = "11:(00)";
    
    ByteBuffer expected = concat(hash("00"), "11");
    boolean delimited = false;
    
    testExpression(input, expected, delimited, label);
  }
  
  
  @Test
  public void testHash5() {
    Object label = new Object() { };

    String input = "11:((00))";
    
    ByteBuffer expected = concat(hash(hash("00")), "11");
    boolean delimited = false;
    
    testExpression(input, expected, delimited, label);
  }
  
  
  @Test
  public void testHash6() {
    Object label = new Object() { };

    String input = "11:(55 (00))";
    
    ByteBuffer expected = concat(hash(concat("55", hash("00"))), "11");
    boolean delimited = false;
    
    testExpression(input, expected, delimited, label);
  }
  
  
  private ByteBuffer concat(ByteBuffer left, String hex) {
    ByteBuffer out = ByteBuffer.allocate(hex.length() / 2 + left.remaining());
    left.mark();
    out.put(left).put(hexToBytes(hex)).flip();
    left.reset();
    return out;
  }
  
  
  private ByteBuffer concat(String hex, ByteBuffer right) {
    ByteBuffer out = ByteBuffer.allocate(hex.length() / 2 + right.remaining());
    right.mark();
    out.put(hexToBytes(hex)).put(right).flip();
    right.reset();
    return out;
  }
  
  
  private ByteBuffer hash(String hex) {
    byte[] bb = hexToBytes(hex);
    return ByteBuffer.wrap(DIGEST.newDigest().digest(bb));
  }
  
  
  private ByteBuffer hash(ByteBuffer value) {
    MessageDigest digest = DIGEST.newDigest();
    value.mark();
    digest.update(value);
    value.reset();
    return ByteBuffer.wrap(digest.digest());
  }
  
  
  @Test
  public void testListedExpr() {
    Object label = new Object() { };
    
    
    String[] dangling =
      {
          "ab:cd",    "cdab",
          "ab:cd 33",    "cdab33",
          "ab:[cd 33]",    "cd33ab",
          "99 :[cd 33:ee]",    "cdee3399",
          "99 :[[cd 33]:ee]",    "eecd3399",
          "bb:99 :[[cd 33]:ee]",    "eecd3399bb",
          "[bb:99] :[[cd 33]:ee]",    "eecd3399bb",
          "[bb:99] :[[cd 33]:ee] 44",    "eecd3399bb44",
      };
    
    String[] delimited =
      {
          "[ab:cd]",    "cdab",
          "[ab:cd 33]",    "cdab33",
          "[ab:[cd 33]]",    "cd33ab",
          "[ 99 :[cD 33:ee]]",    "cdee3399",
          "[[99 :[[cd 33]:ee]]]",    "eecd3399",
          "[bb:99 :[[cd 33]:ee]]",    "eecd3399bb",
          "[[bb:99] :[[cd 33]:ee]]",    "eecd3399bb",
          "[[bb:99] :[[cd 33]:ee] 44]",    "eecd3399bb44",
          
      };
    
    
    
    
    testExpressions(dangling, false, label);
    testExpressions(delimited, true, label);
  }
  
  
  @Test
  public void testMalformeds() {
    Object label = new Object() { };
    String[] inputs =
      {
          "a ",
          " a ",
          "bd [a ] ",
          "a bd ",
          " bd : a ",
          " bd : a ",
          "[:aa bd] ",
          "(:aa bd)",
          "a(aa bd)",
          " :aa bd ",
          " [aa bd ",
          " aa bd )",
          " aa: bd )",
          " aa: bg ",
      };
    testMalformedInputs(inputs, label);
  }
  
  
  @Test
  public void testNestedGroup2() {
    Object label = new Object() { };
    String input = "[ [bd]  ab52 10fe ]";
    String expectedHex = "bdab5210fe";
    boolean delimited = true;
    
    testExpression(input, expectedHex, delimited, label);
  }
  
  
  
  
  
  
  
  
  private void testExpressions(String[] inputExpected, boolean delimited, Object label) {
    for (int index = 0; index < inputExpected.length; index += 2)
      testExpression(inputExpected[index], inputExpected[index + 1], delimited, label);
  }
  
  private void testExpression(String input, String expectedHex, boolean delimited, Object label) {
    testExpression(input, ByteBuffer.wrap(hexToBytes(expectedHex)), delimited, label);
  }
  
  private void testExpression(String input, ByteBuffer expected, boolean delimited, Object label) {
    
    Construct c = Parser.parse(input);
    
    String text = c.toText().toString();
    String canonical = c.canonicalText().toString();
    System.out.println(method(label) + ": " + text);
    if (!text.equals(canonical))
      System.out.println("canonical form: " + canonical);
    System.out.println();
    
    if (!expected.equals(c.bytes())) {
      System.out.println("[ERROR] actual is " + toHex(c.bytes()));
    }
    assertEquals(expected, c.bytes());
    assertEquals(delimited, c.delimited());
  }
  
  
  private void testMalformedInputs(String[] inputs, Object label) {
    for (String input : inputs)
      testMalformed(input, label);
  }
  
  private void testMalformed(String input, Object label) {
    System.out.print(method(label) + ": ");
    try {
      Parser.parse(input);
      
      fail();
    } catch (HashingException expected) {
      System.out.println("[EXPECTED ERROR] " + expected);
      System.out.println();
    }
  }
  
  
  
}












