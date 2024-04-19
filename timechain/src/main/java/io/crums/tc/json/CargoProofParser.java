/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;


import io.crums.sldg.json.HashEncoding;
import io.crums.tc.CargoProof;
import io.crums.util.json.JsonEntityParser;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.simple.JSONObject;

/**
 * Cargo proof parser.
 * 
 * <h2>Historical Note</h2>
 * <p>
 * This used to be called {@code CrumTrailParser}. See
 * {@link CargoProof}.
 * </p>
 */
public class CargoProofParser implements JsonEntityParser<CargoProof> {
  

  private final MerkleProofParser merkleProofParser;
  private final CrumParser crumParser;

  
  public CargoProofParser(HashEncoding hashCodec) {
    this.merkleProofParser = new MerkleProofParser(hashCodec);
    this.crumParser = new CrumParser(hashCodec);
  }
  
  
  

  @Override
  public JSONObject injectEntity(CargoProof cargoProof, JSONObject jObj) {
    merkleProofParser.injectEntity(cargoProof, jObj);
    crumParser.injectEntity(cargoProof.crum(), jObj);
    return jObj;
  }

  @Override
  public CargoProof toEntity(JSONObject jObj) throws JsonParsingException {
    var crum = crumParser.toEntity(jObj);
    var mrklProof = merkleProofParser.toEntity(jObj);
    try {
      return new CargoProof(mrklProof, crum);
    } catch (Exception x) {
      throw new JsonParsingException(x, jObj);
    }
  }

}
