/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;



import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * 
 */
public class StatementTest {
  
  
  @Test
  public void test() {
    
    String[] truths =
      {
          "ab:cd = cdab",
          "ab:cd 33 =cdab 33",
          "ab:[cd 33] = cd 33ab",
          "99 :[cd 33:ee] = cdee3399",
          "bb:99 :[[cd 33]:ee] = ee cd 33 99bb",
          "[bb:99] :[[cd 33]:ee]=\neecd3399bb",
          "[bb:99] :[[cd 33]:ee] 44=\neecd3399bb44",
      };
    
    String[] falsehoods =
      {
          "ab:cd = ab:cd 33",
          "99 :[cd 33:ee] = ab:[cd 33]",
          "bb:99 :[[cd 33]:ee] = ee cd 33 9b",
      };
    
    for (String truth : truths)
      assertStatement(truth, true);
    
    for (String falsehood : falsehoods)
      assertStatement(falsehood, false);
  }
  
  
  
  private void assertStatement(String statement, boolean truth) {
    Statement stmt = Statement.parse(statement);
    boolean result = stmt.eval();
    System.out.println((result ? "TRUE:\n" : "FALSE:\n") + statement);
    assertEquals(truth, result);
  }
  
  

}
