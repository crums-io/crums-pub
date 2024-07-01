/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;


import static io.crums.tc.json.JsonTags.*;

import io.crums.tc.NotaryPolicy;
import io.crums.util.json.JsonEntityParser;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONObject;


/**
 * {@code NotaryPolicy} JSON parser.
 */
public class NotaryPolicyParser implements JsonEntityParser<NotaryPolicy> {

  

  @Override
  public JSONObject injectEntity(NotaryPolicy policy, JSONObject jObj) {
    jObj.put(
        CHAIN_PARAMS,
        ChainParamsParser.INSTANCE.toJsonObject(policy.chainParams()));
    jObj.put(P_BLOCK_COMMIT_LAG, policy.blockCommitLag());
    jObj.put(P_BLOCKS_SEARCHED, policy.blocksSearched());
    jObj.put(P_BLOCKS_RETAINED, policy.blocksRetained());
    return jObj;
  }


  @Override
  public NotaryPolicy toEntity(JSONObject jObj) throws JsonParsingException {
    var params =
      ChainParamsParser.INSTANCE.toEntity(
          JsonUtils.getJsonObject(jObj, CHAIN_PARAMS, true));
    int commitLag = JsonUtils.getInt(jObj, P_BLOCK_COMMIT_LAG);
    int blocksSearched = JsonUtils.getInt(jObj, P_BLOCKS_SEARCHED);
    int blocksRetained = JsonUtils.getInt(jObj, P_BLOCKS_RETAINED);
    try {
      return new NotaryPolicy(params, blocksRetained, commitLag, blocksSearched);
    } catch (Exception x) {
      throw new JsonParsingException(
          "on parsing:\n  " + jObj + "\n detail: " + x, x);
    }
  }

}
