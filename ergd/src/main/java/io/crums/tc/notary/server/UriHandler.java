/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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



  /**
   * Returns a list of newly constructed handlers.
   * 
   * @param notary    the notary (most of) the handers are bound to (others are stateless)
   * @param withUi    if {@code true}, then the static demo pages are also spun up; o.w.
   *                  then only the REST {@code /api/*} endpoints are served
   */
  public static List<UriHandler> all(Notary notary, boolean withUi) {
    // FIXME: none of the handlers depend on server settings (internal to notary)
    var settings = new ServerSettings(notary.settings()); 

    List<UriHandler> out = new ArrayList<>();
    if (withUi)
      out.add(new UriHandler("/", new ResourceHandler()));
    out.add(
        new UriHandler(
            Constants.Rest.POLICY_URI,
            new ApiHandlers.PolicyHandler(notary, settings)));
    out.add(
        new UriHandler(
            Constants.Rest.WITNESS_URI,
            new ApiHandlers.WitnessHandler(notary, settings)));
    out.add(
        new UriHandler(
            Constants.Rest.UPDATE_URI,
            new ApiHandlers.UpdateHandler(notary, settings)));
    out.add(
        new UriHandler(
            Constants.Rest.STATE_URI,
            new ApiHandlers.StateHandler(notary, settings)));
    out.add(
        new UriHandler(
            Constants.Rest.VERIFY_URI,
            new ApiHandlers.VerifyHandler(notary, settings)));
    out.add(
        new UriHandler(
            Constants.Rest.H_CODEC_URI,
            new UtilHandlers.HashConverter()));
    out.add(
        new UriHandler(
            Constants.Rest.DATE_URI,
            new UtilHandlers.UtcDatePrinter()));

    return Collections.unmodifiableList(out);
  }


}


