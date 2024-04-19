/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;

import io.crums.util.json.simple.JSONArray;
import io.crums.util.json.simple.JSONObject;
import io.crums.util.json.simple.parser.JSONParser;
import io.crums.util.json.simple.parser.ParseException;
import io.crums.util.mrkl.Proof;
import io.crums.model.Constants;
import io.crums.util.IntegralStrings;

/**
 * JSON Merkle proof parser.
 */
public class ProofParser {
  

  /**
   * Instances of this class are immutable and stateless.
   */
  public final static ProofParser INSTANCE = new ProofParser();
  

  public final static String INDEX = "index";
  public final static String COUNT = "count";
  public final static String CHAIN = "chain";
  
  
  public JSONObject toJsonObject(Proof proof) {
    
    JSONObject jproof = new JSONObject();
    jproof.put(INDEX, proof.leafIndex());
    jproof.put(COUNT, proof.leafCount());
    
    JSONArray chainArray = new JSONArray();
    for (byte[] link : proof.hashChain())
      chainArray.add(IntegralStrings.toHex(link));
    
    jproof.put(CHAIN, chainArray);
    
    return jproof;
  }
  
  
  
  public Proof toProof(String json) {
    try {
      return toProof((JSONObject) new JSONParser().parse(json));
    } catch (ParseException e) {
      throw new IllegalArgumentException("proof json: " + e.getMessage(), e);
    }
  }
  
  
  public Proof toProof(JSONObject jsonObj) {
    try {
      
      int index = ((Number) jsonObj.get(INDEX)).intValue();
      int count = ((Number) jsonObj.get(COUNT)).intValue();
      
      byte[][] chain;
      {
        JSONArray chainArray = (JSONArray) jsonObj.get(CHAIN);
        chain = new byte[chainArray.size()][];
        for (int i = 0; i < chain.length; ++i)
          chain[i] = IntegralStrings.hexToBytes(chainArray.get(i).toString());
      }
      
      return new Proof(Constants.HASH_ALGO, count, index, chain, false);
      
    } catch (RuntimeException rx) {
      throw new IllegalArgumentException("proof json: " + rx.getMessage(), rx);
    }
  }

}






