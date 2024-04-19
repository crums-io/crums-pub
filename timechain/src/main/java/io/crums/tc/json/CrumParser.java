/*
 * Copyright 2020-2024 Babak Farhang
 */
package io.crums.tc.json;


import static io.crums.tc.json.JsonTags.W_HASH;

import io.crums.sldg.json.HashEncoding;

import static io.crums.tc.json.JsonTags.UTC;

import io.crums.tc.Crum;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONObject;

/**
 * JSON crum parser.
 */
public class CrumParser extends BaseParser<Crum> {

  /**
   * Instances of this class are immutable and stateless.
   */
  public final static CrumParser INSTANCE = new CrumParser();
  
  
  /**
   * Default parser with Base64-32 encoding for hashes.
   */
  public CrumParser() {
    this(HashEncoding.BASE64_32);
  }
  
  /**
   * Creates an instance with the given hash encoding.
   */
  public CrumParser(HashEncoding hashCodec) {
    super(hashCodec);
  }
  
  
  
  
  
  
  
  
  
  

  @Override
  public JSONObject injectEntity(Crum crum, JSONObject jcrum) {
    jcrum.put(W_HASH, hashCodec.encode(crum.hash()));
    jcrum.put(UTC, crum.utc());
    return jcrum;
  }

  
  @Override
  public Crum toEntity(JSONObject jObj) throws JsonParsingException {
    
    long utc = JsonUtils.getNumber(jObj, UTC, true).longValue();
    String encodedHash = JsonUtils.getString(jObj, W_HASH, true);
    
    try {
      
      byte[] hash = hashCodec.decode(encodedHash);
      
      return new Crum(hash, utc);
      
    } catch (RuntimeException rx) {
      throw new JsonParsingException("crum json", rx);
    }
  }

}

