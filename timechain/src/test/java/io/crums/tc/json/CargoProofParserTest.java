/*
 * Copyright 2020-2024 Babak Farhang
 */
package io.crums.tc.json;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.CargoProof;
import io.crums.tc.CargoProofTest;
import io.crums.testing.SelfAwareTestCase;
import io.crums.util.json.JsonPrinter;

/**
 * 
 */
public class CargoProofParserTest extends SelfAwareTestCase {

  
  
  @Test
  public void testRoundtripBase64_32() {
    CargoProof cargoProof = CargoProofTest.randomCargoProof(31_873, 31_875);
    
    CargoProofParser parser = new CargoProofParser(HashEncoding.BASE64_32);
    
    var jObj = parser.toJsonObject(cargoProof);

    System.out.println();
    System.out.println(method(new Object() { }) + ":");
    JsonPrinter.println(jObj);
    
    String json = jObj.toJSONString();
    
    CargoProof rt = parser.toEntity(json);
    
    assertEquals(cargoProof, rt);
    assertEquals(cargoProof.crum(), rt.crum());
    
    
  }
  
  @Test
  public void testRoundtripHex() {
    CargoProof cargoProof = CargoProofTest.randomCargoProof(31_872, 31_875);
    
    CargoProofParser parser = new CargoProofParser(HashEncoding.HEX);
    

    var jObj = parser.toJsonObject(cargoProof);

    System.out.println();
    System.out.println(method(new Object() { }) + ":");
    JsonPrinter.println(jObj);
    
    String json = jObj.toJSONString();
    
    CargoProof rt = parser.toEntity(json);
    
    assertEquals(cargoProof, rt);
    assertEquals(cargoProof.crum(), rt.crum());
  }
  
  

}
