/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;


import static io.crums.tc.json.JsonTags.*;


import io.crums.sldg.json.HashEncoding;
import io.crums.tc.Receipt;
import io.crums.util.json.JsonParsingException;
import io.crums.util.json.JsonUtils;
import io.crums.util.json.simple.JSONObject;

/**
 * Receipt parser. Its JSON representation is a bit strange:
 * it's either a chain-params / crum tuple, or a complete
 * crumtrail.
 */
public class ReceiptParser extends BaseParser<Receipt> {


  public final static ReceiptParser HEX =
    new ReceiptParser(HashEncoding.HEX);

  public final static ReceiptParser B64 =
      new ReceiptParser(HashEncoding.BASE64_32);


  public static ReceiptParser forEncoding(HashEncoding encoding) {
    return encoding == HashEncoding.HEX ? HEX : B64;
  }


  public static ReceiptParser pickParser(JSONObject jReceipt) {
    return forEncoding(inferEncoding(jReceipt));
  }

  
  public static HashEncoding inferEncoding(JSONObject jReceipt)
      throws JsonParsingException {
    
    JSONObject wHashContainer;
    var jCargo = JsonUtils.getJsonObject(jReceipt, CARGO_PROOF, false);
    wHashContainer = jCargo == null ? jReceipt : jCargo;
    
    var whash = JsonUtils.getString(wHashContainer, W_HASH, true);
    return switch (whash.length()) {
    case 43 -> HashEncoding.BASE64_32;
    case 64 -> HashEncoding.HEX;
    default ->  throw new JsonParsingException(
      "cannot parse / encoded hash has invalid length: " + whash);
    };
  }

  
  private final CrumtrailParser trailParser;

  public ReceiptParser(HashEncoding hashCodec) {
    super(hashCodec);
    this.trailParser = new CrumtrailParser(hashCodec);
  }


  @Override
  public JSONObject injectEntity(Receipt receipt, JSONObject jObj) {
    if (receipt.hasTrail()) {
      trailParser.injectEntity(receipt.trail(), jObj);
    } else {
      var jParams = trailParser.paramsParser().toJsonObject(receipt.chainParams());
      jObj.put(CHAIN_PARAMS, jParams);
      trailParser.crumParser().injectEntity(receipt.crum(), jObj);
    }
    return jObj;
  }


  @Override
  public Receipt toEntity(JSONObject jObj) throws JsonParsingException {
    if (jObj.containsKey(BLOCK_PROOF))
      return new Receipt(trailParser.toEntity(jObj));
    
    var jParams = JsonUtils.getJsonObject(jObj, CHAIN_PARAMS, true);
    var chainParams = trailParser.paramsParser().toEntity(jParams);
    var crum = trailParser.crumParser().toEntity(jObj);
    return new Receipt(chainParams, crum);
  }

}
