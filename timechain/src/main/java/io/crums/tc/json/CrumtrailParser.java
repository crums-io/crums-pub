/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;

import static io.crums.tc.json.JsonTags.*;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.Crumtrail;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONObject;

/**
 * 
 */
public class CrumtrailParser extends BaseParser<Crumtrail> {
  
  private final BlockProofParser blockProofParser;
  private final CargoProofParser cargoProofParser;
  private final CrumParser crumParser;

  public CrumtrailParser(HashEncoding hashCodec) {
    super(hashCodec);
    this.blockProofParser = new BlockProofParser(hashCodec);
    this.cargoProofParser = new CargoProofParser(hashCodec);
    this.crumParser = new CrumParser(hashCodec);
  }

  @Override
  public JSONObject injectEntity(Crumtrail trail, JSONObject jObj) {
    var jBlockProof = blockProofParser.toJsonObject(trail.blockProof());
    jObj.put(BLOCK_PROOF, jBlockProof);
    int crumCount = trail.crumsInBlock();
    var jCargoProof = new JSONObject();
    if (crumCount == 1) {
      jCargoProof.put(CRUM_COUNT, 1);
      crumParser.injectEntity(trail.crum(), jCargoProof);
    } else {
      cargoProofParser.injectEntity(
          ((Crumtrail.MerkleTrail) trail).cargoProof(),
          jCargoProof);
    }
    jObj.put(CARGO_PROOF, jCargoProof);
    return jObj;
  }

  @Override
  public Crumtrail toEntity(JSONObject jObj) throws JsonParsingException {
    var jBlockProof = JsonUtils.getJsonObject(jObj, BLOCK_PROOF, true);
    var blockProof = blockProofParser.toEntity(jBlockProof);
    var jCargoProof = JsonUtils.getJsonObject(jObj, CARGO_PROOF, true);
    
    int crumCount = JsonUtils.getInt(jCargoProof, CRUM_COUNT);
    if (crumCount > 1) {
      var cargoProof = cargoProofParser.toEntity(jCargoProof);
      try {
        return Crumtrail.newMerkleTrail(blockProof, cargoProof);
      } catch (Exception x) {
        throw new JsonParsingException(x, jObj);
      }
    } else if (crumCount == 1) {
      var crum = crumParser.toEntity(jCargoProof);
      try {
        return Crumtrail.newLoneTrail(blockProof, crum);
      } catch (Exception x) {
        throw new JsonParsingException(x, jObj);
      }
    }
    
    throw new JsonParsingException(
        "illegal \"" + CRUM_COUNT + "\" (" + crumCount +
        ") on parsing:\n" + jObj);
  }

}
