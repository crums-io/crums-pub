/*
 * Copyright 2020-2024 Babak Farhang
 */
package io.crums.tc.json;


import static io.crums.tc.json.JsonTags.MRKL_PROOF_HASHES;
import static io.crums.tc.json.JsonTags.CRUM_COUNT;
import static io.crums.tc.json.JsonTags.INDEX;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.Constants;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONArray;
import io.crums.util.json.simple.JSONObject;
import io.crums.util.mrkl.Proof;

/**
 * Merkle proof parser.
 * 
 * <h2>Historical Note</h2>
 * <p>
 * This used to be just called {@code ProofParser}. Now that we
 * have other kinds of proofs, we ought to be specific.
 * </p>
 * 
 * @see Proof
 */
public class MerkleProofParser extends BaseParser<Proof> {
  


  public MerkleProofParser(HashEncoding hashCodec) {
    super(hashCodec);
  }
  
  
  
  
  
  
  

  @Override
  public JSONObject injectEntity(Proof proof, JSONObject jObj) {
    jObj.put(CRUM_COUNT, proof.leafCount());
    jObj.put(INDEX, proof.leafIndex());
    
    JSONArray chainArray = new JSONArray();
    for (byte[] link : proof.hashChain())
      chainArray.add(hashCodec.encode(link));
    
    jObj.put(MRKL_PROOF_HASHES, chainArray);
    
    return jObj;
  }

  
  
  @Override
  public Proof toEntity(JSONObject jObj) throws JsonParsingException {

    int index = JsonUtils.getInt(jObj, INDEX);
    int count = JsonUtils.getInt(jObj, CRUM_COUNT);
    JSONArray chainArray = JsonUtils.getJsonArray(jObj, MRKL_PROOF_HASHES, true);
    
    try {
      
      byte[][] chain;
      {
        chain = new byte[chainArray.size()][];
        for (int i = 0; i < chain.length; ++i)
          chain[i] = hashCodec.decode(chainArray.get(i).toString());
      }
      
      return new Proof(Constants.HASH_ALGO, count, index, chain, false);
      
    } catch (RuntimeException rx) {
      throw new JsonParsingException("proof json: " + rx.getMessage(), rx);
    }
  }

}
