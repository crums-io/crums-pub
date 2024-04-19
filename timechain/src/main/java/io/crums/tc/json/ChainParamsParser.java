/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;

import io.crums.tc.ChainParams;
import io.crums.tc.TimeBinner;
import io.crums.util.json.JsonEntityParser;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONObject;

/**
 * 
 */
public class ChainParamsParser implements JsonEntityParser<ChainParams> {
  
  public final static ChainParamsParser INSTANCE = new ChainParamsParser();

  @Override
  public JSONObject injectEntity(ChainParams params, JSONObject jObj) {
    jObj.put(JsonTags.CN_INCEPT_UTC, params.inceptionUtc());
    jObj.put(JsonTags.CN_BIN_EXP, params.timeBinner().binExponent());
    return jObj;
  }

  @Override
  public ChainParams toEntity(JSONObject jObj) throws JsonParsingException {
    try {
      long inceptUtc =
          JsonUtils.getNumber(jObj, JsonTags.CN_INCEPT_UTC, true).longValue();
      int binExponent = JsonUtils.getInt(jObj, JsonTags.CN_BIN_EXP);
      var binner = TimeBinner.forExponent(binExponent);
      return new ChainParams(binner, inceptUtc);
    
    } catch (JsonParsingException jpx) {
      throw jpx;
    } catch (Exception x) {
      throw new JsonParsingException(x, jObj);
    }
  }

}
