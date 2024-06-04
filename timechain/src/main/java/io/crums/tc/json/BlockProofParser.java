/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;


import static io.crums.tc.json.JsonTags.*;

import io.crums.sldg.json.HashEncoding;
import io.crums.sldg.json.PathPackParser;
import io.crums.tc.BlockProof;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONObject;

/**
 * 
 */
public class BlockProofParser extends BaseParser<BlockProof> {


  public final static BlockProofParser B64 =
      new BlockProofParser(HashEncoding.BASE64_32);

  public final static BlockProofParser HEX =
      new BlockProofParser(HashEncoding.HEX);

  
  public static BlockProofParser forEncoding(HashEncoding encoding) {
    return encoding == HashEncoding.HEX ? HEX : B64;
  }

  private final ChainParamsParser paramsParser = ChainParamsParser.INSTANCE;
  private final PathPackParser pathParser;
  

  public BlockProofParser(HashEncoding hashCodec) {
    super(hashCodec);
    this.pathParser = new PathPackParser(
        hashCodec, BP_STITCH_NOS, BP_HASHES);
  }


  public final ChainParamsParser paramsParser() {
    return paramsParser;
  }

  @Override
  public JSONObject injectEntity(BlockProof proof, JSONObject jObj) {
    var jParams = paramsParser.toJsonObject(proof.chainParams());
    jObj.put(CHAIN_PARAMS, jParams);
    
    pathParser.injectEntity(proof.chainState().pack(), jObj);
    return jObj;
  }

  @Override
  public BlockProof toEntity(JSONObject jObj) throws JsonParsingException {
    try {
      var jParams = JsonUtils.getJsonObject(jObj, CHAIN_PARAMS, true);
      var params = paramsParser.toEntity(jParams);
      
      var chainState = pathParser.toEntity(jObj).path();
      return new BlockProof(params, chainState);
      
    } catch (JsonParsingException jpx) {
      throw jpx;
    } catch (Exception x) {
      throw new JsonParsingException(x, jObj);
    }
  }

}
