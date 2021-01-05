/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.crums.model.Crum;
import io.crums.util.IntegralStrings;

/**
 * JSON crum parser.
 */
public class CrumParser {

  /**
   * Instances of this class are immutable and stateless.
   */
  public final static CrumParser INSTANCE = new CrumParser();
  
  
  public final static String UTC = "utc";
  public final static String HASH = "hash";
  
  
  
  @SuppressWarnings("unchecked")
  public JSONObject toJsonObject(Crum crum) {
    
    JSONObject jcrum = new JSONObject();
    jcrum.put(UTC, crum.utc());
    jcrum.put(HASH, IntegralStrings.toHex(crum.hash()));
    
    return jcrum;
  }
  
  
  
  
  public Crum toCrum(String json) {
    try {
      return toCrum((JSONObject) new JSONParser().parse(json));
    } catch (ParseException e) {
      throw new IllegalArgumentException("crum json: " + e.getMessage(), e);
    }
  }
  
  
  public Crum toCrum(JSONObject jsonObj) throws IllegalArgumentException {
    try {
      
      long utc = (Long) jsonObj.get(UTC);
      byte[] hash = IntegralStrings.hexToBytes(jsonObj.get(HASH).toString());
      
      return new Crum(hash, utc);
      
    } catch (RuntimeException rx) {
      throw new IllegalArgumentException("crum json", rx);
    }
  }

}
