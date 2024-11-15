/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;

import java.util.Objects;

import com.sun.net.httpserver.HttpHandler;

import io.crums.tc.Constants;
import io.crums.tc.notary.Notary;


/** Pairs a URI with an {@code HttpHandler}. */
public record UriHandler(String uri, HttpHandler handler) {

  public UriHandler {
    if (!uri.startsWith("/"))
      throw new IllegalArgumentException("uri must begin with '/': " + uri);
    Objects.requireNonNull(handler, "null handler");
  }


  public static UriHandler[] all(Notary notary) {
    return all(notary, new ServerSettings(notary.settings()));
  }


  public static UriHandler[] all(Notary notary, ServerSettings settings) {
    
    UriHandler[] all = {
      new UriHandler(
        "/",
        new ResourceHandler()),
      new UriHandler(
        Constants.Rest.POLICY_URI,
        new ApiHandlers.PolicyHandler(notary, settings)),
      new UriHandler(
        Constants.Rest.WITNESS_URI,
        new ApiHandlers.WitnessHandler(notary, settings)),
      new UriHandler(
        Constants.Rest.UPDATE_URI,
        new ApiHandlers.UpdateHandler(notary, settings)),
      new UriHandler(
        Constants.Rest.STATE_URI,
        new ApiHandlers.StateHandler(notary, settings)),
      new UriHandler(
        Constants.Rest.VERIFY_URI,
        new ApiHandlers.VerifyHandler(notary, settings)),
      new UriHandler(
        Constants.Rest.H_CODEC_URI,
        new UtilHandlers.HashConverter()),
      new UriHandler(
        Constants.Rest.DATE_URI,
        new UtilHandlers.UtcDatePrinter()),
      
    };

    return all;
  }


}


