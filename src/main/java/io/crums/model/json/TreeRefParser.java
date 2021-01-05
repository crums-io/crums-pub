/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;


import java.nio.ByteBuffer;
import java.util.Objects;

import org.json.simple.JSONObject;

import io.crums.model.TreeRef;
import io.crums.model.Constants;
import io.crums.util.IntegralStrings;


/**
 * Parses and generates {@linkplain TreeRef} JSON.
 */
public class TreeRefParser {
  
  
  public final static TreeRefParser INSTANCE = new TreeRefParser();
  
  


  @SuppressWarnings("unchecked")
  public JSONObject toJsonObject(TreeRef ref) {
    JSONObject jtree = new JSONObject();
    jtree.put(Constants.RSP_HASH_JSON, ref.hashHex());
    jtree.put(Constants.RSP_MIN_UTC, ref.minUtc());
    jtree.put(Constants.RSP_MAX_UTC, ref.maxUtc());
    jtree.put(Constants.RSP_TREE_NUM, ref.treeNumber());
    return jtree;
  }
  
  
  
  
  public TreeRef toTreeRef(JSONObject jtree) {
    Objects.requireNonNull(jtree, "null JSONObject");
    try {
      String hashHex = jtree.get(Constants.RSP_HASH_JSON).toString();
      byte[] hash = IntegralStrings.hexToBytes(hashHex);
      long minUtc = (Long) jtree.get(Constants.RSP_MIN_UTC);
      long maxUtc = (Long) jtree.get(Constants.RSP_MAX_UTC);
      int treeNumber = ((Long) jtree.get(Constants.RSP_TREE_NUM)).intValue();
      
      ByteBuffer data = ByteBuffer.allocate(TreeRef.TREE_REF_SIZE);
      TreeRef.writeTreeRefToBuffer(hash, minUtc, maxUtc, data).flip();
      
      return new TreeRef(data, treeNumber);
      
    } catch (Exception x) {
      String json = jtree.toJSONString();
      if (json.length() > 250)
        json = json.substring(0, 250) + "...[truncated]";
      throw new IllegalArgumentException(
          "failed JSON conversion: '" + json + "'", x);
    }
  }

}
