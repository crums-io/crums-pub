/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.crums.tc.Crum;
import io.crums.tc.Constants;



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
    
    Crum rt = parser.toEntity(out);

    assertEquals(expected, rt);
  }

}

