/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;


import static io.crums.tc.notary.server.ApiHandlers.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.Constants;

/**
 * HTTP utility handlers.
 * <ul>
 * <li>Hash encoding</li>
 * <li>UTC date conversion</li>
 * <li>Verify</li>
 * </ul>
 */
public class UtilHandlers {



  public static class HashConverter implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!HttpServerHelp.screenGetOnly(exchange))
        return;
      
      var queryMap = HttpServerHelp.queryMap(exchange);
      
      var strHash =
          HttpServerHelp.requiredSingleValue(
              queryMap,
              Constants.Rest.QS_HASH,
              exchange);
      
      if (strHash == null)
        return;

      HashEncoding enc = pickEncoding(List.of(strHash), exchange);
      if (enc == null)
        return;
      
      var hash = toBuffer(strHash, enc, exchange);
      if (hash == null)
        return;

      HashEncoding otherEnc = switch (enc) {
        case HEX        -> HashEncoding.BASE64_32;
        case BASE64_32  -> HashEncoding.HEX;
      };
      
      HttpServerHelp.sendText(exchange, 200, otherEnc.encode(hash));
    }





    
  }




  public static class UtcDatePrinter implements HttpHandler {

    final static String DATE_FORMAT = "yyyy-MM-ddTHH:mm:ss";

    final static TimeZone GMT = TimeZone.getTimeZone("GMT");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!HttpServerHelp.screenGetOnly(exchange))
        return;
      
      var queryMap = HttpServerHelp.queryMap(exchange);
      
      long utc = HttpServerHelp.requiredLongValue(
          queryMap, Constants.Rest.QS_UTC, exchange, 0);
    
      if (utc == 0)
        return;

      var format = new SimpleDateFormat(DATE_FORMAT);
      format.setTimeZone(GMT);

      HttpServerHelp.sendText(exchange, 200, format.format(new Date(utc)));
    }

  }





}
