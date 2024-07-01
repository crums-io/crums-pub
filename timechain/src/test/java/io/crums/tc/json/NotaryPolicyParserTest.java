/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.crums.tc.ChainParams;
import io.crums.tc.NotaryPolicy;
import io.crums.tc.TimeBinner;
import io.crums.testing.SelfAwareTestCase;
import io.crums.util.json.JsonPrinter;


/**
 * 
 */
public class NotaryPolicyParserTest extends SelfAwareTestCase {

  @Test
  public void testRoundTrip() throws Exception {
    final Object label = new Object() { };
    final long now = System.currentTimeMillis();
    System.out.println(method(label) + ": UTC now = " + now);
    var params = ChainParams.forStartUtc(TimeBinner.HALF_MINUTE, now);
    var policy = new NotaryPolicy(params, 78, 1616, 1);
    var parser = new NotaryPolicyParser();

    var jPolicy = parser.toJsonObject(policy);
    JsonPrinter.println(jPolicy);
    assertTrue(policy.equalPolicy(parser.toEntity(jPolicy)));
  }

}

