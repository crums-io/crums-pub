/*
 * Copyright 2022 Babak Farhang
 */
package io.crums.tc.notary.server;


import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

import io.crums.util.Strings;

/**
 * Utility methods for {@code HttpHandler}s.
 */
public class HttpServerHelp {
  // no one calls
  private HttpServerHelp() {  }
  
  
  public enum MimeType {
    HTML("text/html; charset=UTF-8"),
    TEXT("text/plain; charset=UTF-8"),
    CSS("text/css; charset=UTF-8"),
    JS("text/javascript; charset=UTF-8"),
    PNG("image/png"),
    GIF("image/gif"),
    JPEG("image/jpeg"),
    ICO("image/x-icon"),
    SVG("image/svg+xml"),
    JSON("application/json; charset=UTF-8"),
    XML("text/xml; charset=UTF-8");
    
    private final String mime;
    
    private MimeType(String mime) {
      this.mime = mime;
    }
    
    public String mime() {
      return mime;
    }
    
    public void setContentType(HttpExchange exchange) {
      HttpServerHelp.setContentType(exchange, mime);
    }
  }
  
  
  
  
  /**
   * Sets the content type, if not blank or {@code null}.
   */
  public static void setContentType(HttpExchange exchange, String contentType) {
    if (contentType == null || contentType.isBlank())
      return;
    exchange.getResponseHeaders().set("Content-Type", contentType);
  }
  
  
  public static void sendNotFound(HttpExchange exchange, String msg)
      throws IOException {
    sendText(exchange, 404, msg);
  }
  
  
  /**
   * Sends 400 level bad-request message. Terminal operation.
   */
  public static void sendBadRequest(HttpExchange exchange, String msg)
      throws IOException {
    sendText(exchange, 400, msg);
  }
  
  /**
   * Sends the given text. Terminal operation.
   */
  public static void sendText(
      HttpExchange exchange, int httpStatus, String msg)
      throws IOException {

    var msgBytes = msg.getBytes(Strings.UTF_8);
    MimeType.TEXT.setContentType(exchange);
    exchange.sendResponseHeaders(httpStatus, msgBytes.length);
    try (var out = exchange.getResponseBody()) {
      out.write(msgBytes);
    }
  }
  
  /**
   * Sends the given JSON. Terminal operation.
   * 
   * @param json  its {@code toString()} is expected to emit JSON
   */
  public static void sendJson(
      HttpExchange exchange, int httpStatus, Object json)
      throws IOException {
    
    var msgBytes = json.toString().getBytes(Strings.UTF_8);
    MimeType.JSON.setContentType(exchange);
    exchange.sendResponseHeaders(httpStatus, msgBytes.length);
    try (var out = exchange.getResponseBody()) {
      out.write(msgBytes);
    }
  }
  
  
  /**
   * Returns {@code true} if the HTTP request-method is 'GET'.
   */
  public static boolean isGetMethod(HttpExchange exchange) {
    return "GET".equals(exchange.getRequestMethod().toUpperCase());
  }

  
  
  /**
   * Screens the {@code exchange}, and if it's a 'GET', returns
   * {@code true}. Otherwise, this becomes a terminal operation:
   * a {@linkplain #sendBadRequest(HttpExchange, String) bad-request}
   * is sent and {@code false} is returned.
   */
  public static boolean screenGetOnly(HttpExchange exchange)
      throws IOException {
    
    if (isGetMethod(exchange))
      return true;
    sendBadRequest(
        exchange,
        "Only 'GET' is allowed; HTTP verb: '" + exchange.getRequestMethod() + "'");
    return false;
  }
  
  

  /**
   * Returns the query string in the given {@code exchange} as a
   * "multi-valued" map.
   * 
   * @return not null, but possibly empty
   */
  public static Map<String, List<String>> queryMap(HttpExchange exchange) {
    return queryMap(exchange.getRequestURI().getRawQuery());
  }
  
  
  /**
   * Returns the given {@code query} string as a "multi-valued" map.
   * 
   * @return not null, but possibly empty
   */
  public static Map<String, List<String>> queryMap(String query) {
    if (query == null || query.isEmpty()) {
      return Map.of();
    }
    Map<String, List<String>> result = new HashMap<>();
    for (String param : query.split("&")) {
      String[] entry = param.split("=");
      var name = entry[0];
      var list = result.get(name);
      if (list == null) {
        list = new ArrayList<>(2);
        result.put(name, list);
      }
      String value  = entry.length > 1 ?
          URLDecoder.decode(entry[1], Strings.UTF_8) : "";
      
      list.add(value);
    }
    return result;
  }
  
  /**
   * Returns the required value[s] for the given {@code key}. If the
   * value[s] doesn't exist, a bad-request (400) message is first sent,
   * before {@code null} is returned.
   */
  public static List<String> requiredValues(
      Map<String, List<String>> queryMap, String key, HttpExchange exchange)
          throws IOException {
    
    var values = queryMap.get(key);
    if (values == null || values.isEmpty()) {
      // (latter condition should never happen, btw)
      sendBadRequest(
          exchange,
          "Missing expected query string value for '" + key + "'");
      return null;
    }
    return values;
  }
  

  /**
   * Returns the required value for the given {@code key}. If the
   * value doesn't exist, or if there is more than one value for
   * the given key, then a bad-request (400) response is first sent,
   * before {@code null} is returned.
   */
  public static String requiredSingleValue(
      Map<String, List<String>> queryMap, String key, HttpExchange exchange)
          throws IOException {
    
    var values = requiredValues(queryMap, key, exchange);
    if (values == null)
      return null;
    if (values.size() > 1) {
      sendBadRequest(
          exchange,
          "Only a single value may be set for '" + key +
          "' in query string; actual given was " + values);
      return null;
    }
    return values.get(0);
  }
  
  
  
  
  /**
   * Returns the required integral value for the given {@code key}. If
   * not found, or malformed, then a bad-request (400) reponse is first sent,
   * before the given {@code errorCode} is returned. (If the value is not
   * malformed, but happens to be the error code, then a bad-request response
   * is sent, proclaiming the given value was illegal.)
   */
  public static long requiredLongValue(
      Map<String, List<String>> queryMap, String key, HttpExchange exchange,
      long errorCode)
          throws IOException {
    
    var strValue = requiredSingleValue(queryMap, key, exchange);
    if (strValue == null)
      return errorCode;
    
    
    try {
      long value = Long.parseLong(strValue);
      if (value == errorCode)
        sendBadRequest(
            exchange,
            "Illegal query string value: " + key + "=" + strValue);
      return value;
    
    } catch (Exception x) {
      sendBadRequest(
          exchange,
          "Malformed integral query string value: " + key + "=" + strValue);
      return errorCode;
    }
  }
  
  
  
  /**
   * Returns the optional integral value, or {@code null} if there is an
   * error. If there is an error, a bad-request (400) response is sent
   * (terminal operation), before returning {@code null}.
   */
  public static Optional<Long> optionalLongValue(
      Map<String, List<String>> queryMap, String key, HttpExchange exchange)
          throws IOException {

    var strOpt = optionalStringValue(queryMap, key, exchange);
    if (strOpt == null)
      return null;
    else if (strOpt.isEmpty())
      return Optional.empty();
    
    try {
      return Optional.of( Long.parseLong(strOpt.get()));
    
    } catch (Exception x) {
      sendBadRequest(
          exchange,
          "Malformed integral query string value: " + key + "=" + strOpt.get());
      return null;
    }
    
  }




  public static Optional<String> optionalStringValue(
      Map<String, List<String>> queryMap, String key, HttpExchange exchange)
          throws IOException {
    
    var strList = queryMap.get(key);
    if (strList == null || strList.isEmpty())
      return Optional.empty();
    
    if (strList.size() != 1) {
      sendBadRequest(
          exchange,
          "Multiple query string values for '" + key + "': " + strList);
      return null;
    }
    return Optional.of(strList.get(0));
    
  
  }



  public static Optional<Boolean> optionalBooleanValue(
      Map<String, List<String>> queryMap, String key, HttpExchange exchange)
          throws IOException {
  
    var strOpt = optionalStringValue(queryMap, key, exchange);
    if (strOpt == null)
      return null;
    if (strOpt.isEmpty())
      return Optional.empty();
    return Optional.of(Boolean.parseBoolean(strOpt.get()));
  }
  
  

}























