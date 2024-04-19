/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.model.Crum;
import io.crums.model.Constants;



/**
 * 
 */
public class CrumParserTest {
  


  @Test
  public void testRoundTrip() throws Exception {
    CrumParser parser = new CrumParser();

    long utc = Crum.INCEPTION_UTC + 123;
    byte[] hash = new byte[Constants.HASH_WIDTH];
    
    Random rand = new Random(12);
    rand.nextBytes(hash);
    
    Crum expected = new Crum(hash, utc);
    
    String out = parser.toJsonObject(expected).toJSONString();
    System.out.println(out);
    
    Crum rt = parser.toCrum(out);

    assertEquals(expected, rt);
  }

}
