/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model.json;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.crums.util.json.simple.JSONArray;
import io.crums.util.json.simple.JSONObject;
import io.crums.util.json.simple.parser.JSONParser;
import io.crums.util.json.simple.parser.ParseException;
import io.crums.util.mrkl.Proof;
import io.crums.model.Crum;
import io.crums.model.CrumRecord;
import io.crums.model.CrumTrail;
import io.crums.model.TrailedRecord;
import io.crums.model.UntrailedRecord;
import io.crums.model.Constants;
import io.crums.util.Lists;

/**
 * JSON crum trail parser.
 * 
 * @see #proofParser()
 * @see #crumParser()
 */
public class CrumTrailParser {
  

  /**
   * Instances of this class are immutable and stateless.
   */
  public final static CrumTrailParser INSTANCE = new CrumTrailParser();
  
  public final static String PROOF = Constants.JSON_PROOF;
  public final static String CRUM = Constants.JSON_CRUM;
  
  
  
  
  
  
  private final ProofParser proofParser;
  private final CrumParser crumParser;
  
  
  
  
  public CrumTrailParser() {
    this(ProofParser.INSTANCE, CrumParser.INSTANCE);
  }
  
  
  public CrumTrailParser(ProofParser proofParser, CrumParser crumParser) {
    this.proofParser = Objects.requireNonNull(proofParser, "null proofParser");
    this.crumParser = Objects.requireNonNull(crumParser, "null crumParser");
  }
  
  
  

  public JSONObject toJsonObject(CrumTrail trail) {
    
    JSONObject jtrail = new JSONObject();
    jtrail.put(PROOF, proofParser.toJsonObject(trail));
    jtrail.put(CRUM, crumParser.toJsonObject(trail.crum()));
    
    
    return jtrail;
  }
  
  
  public CrumTrail toCrumTrail(String json) {
    try {
      return toCrumTrail((JSONObject) new JSONParser().parse(json));
    } catch (ParseException e) {
      throw new IllegalArgumentException("failed to parse json: " + json, e);
    } catch (ClassCastException ccx) {
      throw new IllegalArgumentException("expected object; instead was array: " + json);
    }
  }
  
  
  public CrumTrail toCrumTrail(JSONObject jtrail) {
    JSONObject jproof, jcrum;
    try {
      jproof = (JSONObject) jtrail.get(PROOF);
      jcrum = (JSONObject) jtrail.get(CRUM);
    } catch (ClassCastException ccx) {
      throw new IllegalArgumentException("crumtrail json: " + jtrail);
    }
    if (jproof == null)
      throw new IllegalArgumentException("crumtrail json: missing " + PROOF + " tag: " + jtrail);
    if (jcrum == null)
      throw new IllegalArgumentException("crumtrail json: missing " + CRUM + " tag: " + jtrail);
    
    Proof proof = proofParser.toProof(jproof);
    Crum crum = crumParser.toCrum(jcrum);
    
    return new CrumTrail(proof, crum);
  }
  
  
  /**
   * Returns the optionally trailed <tt>CrumRecord</tt>.
   * 
   * @param jtrail
   * @return
   */
  public CrumRecord toCrumRecord(JSONObject jtrail) {
    JSONObject jproof, jcrum;
    try {
      jproof = (JSONObject) jtrail.get(PROOF);
      jcrum = (JSONObject) jtrail.get(CRUM);
    } catch (ClassCastException ccx) {
      throw new IllegalArgumentException("crumtrail json: " + jtrail);
    }
    if (jcrum == null)
      throw new IllegalArgumentException("crumtrail json: missing " + CRUM + " tag: " + jtrail);

    Crum crum = crumParser.toCrum(jcrum);
    if (jproof == null)
      return new UntrailedRecord(crum);
    
    Proof proof = proofParser.toProof(jproof);
    return new TrailedRecord(new CrumTrail(proof, crum));
  }
  
  
  /**
   * Converts and returns the given string as a list of {@linkplain CrumRecord}s.
   * In the case of a single record, there is flexibility in the input: it
   * may either be a singleton JSON array, or just a JSON object.
   * @param json
   * @return
   */
  public List<CrumRecord> toCrumRecords(String json) {
    try {
      Object oa = new JSONParser().parse(json);
      return toCrumRecords(oa);
    } catch (ParseException px) {
      throw new IllegalArgumentException("failed to parse json: " + json, px);
    }
  }
  
  
  
  public List<CrumRecord> toCrumRecords(Object oa) {
    
    if (oa instanceof JSONObject)
      return Collections.singletonList(toCrumRecord((JSONObject) oa));
    
    else if (!(oa instanceof JSONArray))
      throw new IllegalArgumentException("not an org.json.simple object: " + oa);

    JSONArray array = (JSONArray) oa;
    CrumRecord[] records = new CrumRecord[array.size()];
    try {

      for (int index = 0; index < records.length; ++index)
        records[index] = toCrumRecord((JSONObject) array.get(index));
      
    } catch (ClassCastException ccx) {
      throw new IllegalArgumentException("expected object; instead was array");
    }
    
    return Lists.asReadOnlyList(records);
    
  }
  
  
  
  public ProofParser proofParser() {
    return proofParser;
  }
  
  
  public CrumParser crumParser() {
    return crumParser;
  }

}

































