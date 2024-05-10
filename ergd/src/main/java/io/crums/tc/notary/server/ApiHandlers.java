/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.BlockProof;
import io.crums.tc.Constants;
import io.crums.tc.Crum;
import io.crums.tc.json.BlockProofParser;
import io.crums.tc.json.CrumParser;
import io.crums.tc.json.CrumtrailParser;
import io.crums.tc.notary.Notary;
import io.crums.tc.notary.Receipt;
import io.crums.util.json.simple.JSONArray;

/**
 * 
 */
public class ApiHandlers {
  
  
  final static String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  final static String HTTP_DATE_GMT_TIMEZONE = "GMT";
  
  
  

  
  private final static CrumtrailParser TRAIL_PARSER_B64 =
      new CrumtrailParser(HashEncoding.BASE64_32);
  
  private final static CrumtrailParser TRAIL_PARSER_HEX =
      new CrumtrailParser(HashEncoding.HEX);
  
  
  private static CrumtrailParser trailParser(HashEncoding encoding) {
    switch (encoding) {
    case BASE64_32:   return TRAIL_PARSER_B64;
    case HEX:         return TRAIL_PARSER_HEX;
    default:
      throw new AssertionError("unaccounted: " + encoding);
    }
  }
  
  
  private final static CrumParser CRUM_PARSER_B64 =
      new CrumParser(HashEncoding.BASE64_32);
  
  private final static CrumParser CRUM_PARSER_HEX =
      new CrumParser(HashEncoding.HEX);
  
  
  
  private static CrumParser crumParser(HashEncoding encoding) {
    switch (encoding) {
    case BASE64_32:   return CRUM_PARSER_B64;
    case HEX:         return CRUM_PARSER_HEX;
    default:
      throw new AssertionError("unaccounted: " + encoding);
    }
  }
  
  

  
  
  
  static abstract class Base implements HttpHandler {
    
    final Notary notary;
    final ServerSettings settings;
    final SimpleDateFormat dateFormatter;
    
    
    Base(Notary notary, ServerSettings settings) {
      this.notary = notary;
      this.settings = Objects.requireNonNull(settings);
      this.dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
      dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));
      if (!notary.isOpen())
        throw new IllegalArgumentException(notary + " is closed");
    }
    

    /**
     * Returns the list of hashes in the query string.
     * If the returned list is empty, then a 400 level bad-request
     * has already been sent and there is nothing more to do.
     */
    List<String> stringHashes(
        Map<String, List<String>> queryMap, HttpExchange exchange)
            throws IOException {
    
      var hashList = queryMap.get(Constants.Rest.QS_HASH);
      if (hashList == null || hashList.isEmpty()) {
        HttpServerHelp.sendBadRequest(
            exchange,
            "required '" + Constants.Rest.QS_HASH + "' parameter missing");
        return List.of();
      }
      
      if (hashList.size() > settings.maxHashesPerWitness()) {
        HttpServerHelp.sendBadRequest(
            exchange,
            "no. of submitted hashes (" + hashList.size() +
            ") exceeds maximum (" + settings.maxHashesPerWitness() + ")");
        return List.of();
      }
        
      
      return hashList;
    }
    

    
    /**
     * Returns the optional encoding. If {@code null} is returned, then it
     * was a bad request (400) and was already handled.
     */
    Optional<HashEncoding> getEncoding(
        Map<String, List<String>> queryMap, HttpExchange exchange)
            throws IOException {
      
      var encodingStrs = queryMap.get(Constants.Rest.QS_ENCODING);
      if (encodingStrs == null || encodingStrs.isEmpty())
        return Optional.empty();
      
      if (encodingStrs.size() > 1) {
        HttpServerHelp.sendBadRequest(
            exchange,
            Constants.Rest.QS_ENCODING + " is set more than once (" +
            encodingStrs.size() + ")");
        return null;
      }
      var encoding = encodingStrs.get(0);
      if (Constants.Rest.B64.equals(encoding))
        return Optional.of(HashEncoding.BASE64_32);
      else if (Constants.Rest.HEX.equals(encoding))
        return Optional.of(HashEncoding.HEX);
      
      HttpServerHelp.sendBadRequest(
          exchange,
          "unknown hash encoding: " + Constants.Rest.QS_ENCODING + "=" + encoding);
      return null;
    }
    

    /**
     * Picks the encoding from the given list of hashes. It must be
     * one or the other; mixed encodings are not supported. If one cannot
     * be picked, then a bad-request (400) is sent, and null is returned.
     */
    HashEncoding pickEncoding(
        List<String> hashes, HttpExchange exchange)
            throws IOException {
      
      final int len = hashes.get(0).length();
      final HashEncoding encoding;
      if (len == 43) {
        encoding = HashEncoding.BASE64_32;
      } else if (len == 64) {
        encoding = HashEncoding.HEX;
      } else {
        HttpServerHelp.sendBadRequest(
            exchange,
            "does not parse to 32-byte hash: " + Constants.Rest.QS_HASH +
            "=" + hashes.get(0));
        return null;
      }
      for (int index = hashes.size(); index-- > 1; ) {
        int len2 = hashes.get(index).length();
        if (len2 != len) {
          String msg;
          if (len2 + len == 43 + 64) {
            msg = 
                "mixed hash encodings are not supported: " +
                Constants.Rest.QS_HASH + "=" + hashes.get(0) + " ; " +
                Constants.Rest.QS_HASH + "=" + hashes.get(index);
          } else {
            msg =
                "does not parse to 32-byte hash: " + Constants.Rest.QS_HASH +
                "=" + hashes.get(index);
          }

          HttpServerHelp.sendBadRequest(exchange,msg);
          return null;
        } // if (len2 != len
      } // for
      
      return encoding;
    }
    
    
    
    /**
     * Returns the given {@code hash} as a 32-byte buffer. If malformed,
     * then a bad-request (400) is sent and {@code null} is returned.
     */
    ByteBuffer toBuffer(
        String hash, HashEncoding encoding, HttpExchange exchange)
            throws IOException {
      
      try {
        return ByteBuffer.wrap(encoding.decode(hash));
      
      } catch (Exception x) {
        HttpServerHelp.sendBadRequest(
            exchange,
            "does not parse to 32-byte hash: " + Constants.Rest.QS_HASH +
            "=" + hash);
        return null;
      }
    }
    
    
  }
  
  
  

  /** Handler for the "witness" URI endpoint. */
  public static class WitnessHandler extends Base {
    
    public WitnessHandler(Notary notary, ServerSettings settings) {
      super(notary, settings);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!HttpServerHelp.screenGetOnly(exchange))
        return;
      
      var queryMap = HttpServerHelp.queryMap(exchange);
      
      var strHashes = stringHashes(queryMap, exchange);
      if (strHashes.isEmpty())
        return;
      
      Optional<HashEncoding> encOpt = getEncoding(queryMap, exchange);
      if (encOpt == null)
        return;
      
      HashEncoding enc = pickEncoding(strHashes, exchange);
      if (enc == null)
        return;
      
      List<ByteBuffer> hashes;

      if (strHashes.size() == 1) {
        var buf = toBuffer(strHashes.get(0), enc, exchange);
        if (buf == null)
          return;
        hashes = List.of(buf);
      } else {
        hashes = new ArrayList<>(strHashes.size());
        for (var sh : strHashes) {
          var buf = toBuffer(sh, enc, exchange);
          if (buf == null)
            return;
          hashes.add(buf);
        }
      }
      
      List<Receipt> receipts;
      try {
        if (hashes.size() == 1)
          receipts = List.of(notary.witness(hashes.get(0)));
        else {
          receipts = new ArrayList<>(hashes.size());
          for (var hash : hashes)
            receipts.add(notary.witness(hash));
        }
      } catch (Exception x) {
        HttpServerHelp.sendText(
            exchange, 500, "internal server error: " + x.getMessage());
        return;
      }
      
      HashEncoding outCodec = encOpt.orElse(HashEncoding.BASE64_32);
      Object json;
      int status;
      if (receipts.size() == 1) {
        var rcpt = receipts.get(0);
        if (rcpt.hasTrail()) {
          status = 200;
          json = trailParser(outCodec).toJsonObject(rcpt.trail());
        } else {
          status = 202;
          json = crumParser(outCodec).toJsonObject(rcpt.crum());
        }
      
      } else {
        var jArray = new JSONArray(receipts.size());
        var trailParser = trailParser(outCodec);
        var crumParser = crumParser(outCodec);
        status = 200;
        for (var rcpt : receipts) {
          Object item;
          if (rcpt.hasTrail())
            item = trailParser.toJsonObject(rcpt.trail());
          else {
            status = 202;
            item = crumParser.toJsonObject(rcpt.crum());
          }
          jArray.add(item);
        }
        json = jArray;
      }
      
      HttpServerHelp.sendJson(exchange, status, json);
    }
    

  } // class WitnessHandler
    
    

  /** Handler for the "update" URI endpoint. */
  public static class UpdateHandler extends Base {

    UpdateHandler(Notary notary, ServerSettings settings) {
      super(notary, settings);
    }
    
    

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
      
      

      Optional<HashEncoding> encOpt = getEncoding(queryMap, exchange);
      if (encOpt == null)
        return;
      
      HashEncoding enc = pickEncoding(List.of(strHash), exchange);
      if (enc == null)
        return;
      
      var hash = toBuffer(strHash, enc, exchange);
      if (hash == null)
        return;
      
      long utc = getUtc(queryMap, exchange, 0);
      if (utc == 0)
        return;
      
      Crum crum = new Crum(hash, utc);
      
      Receipt rcpt;
      try {
        rcpt = notary.update(crum);
      
      } catch (Exception x) {
        HttpServerHelp.sendText(
            exchange, 500, "internal server error: " + x.getMessage());
        return;
      }
      
      HashEncoding outCodec = encOpt.orElse(HashEncoding.BASE64_32);
      Object json;
      if (rcpt.hasTrail())
        json = trailParser(outCodec).toJsonObject(rcpt.trail());
      else
        json = crumParser(outCodec).toJsonObject(rcpt.crum());
      
      HttpServerHelp.sendJson(exchange, 200, json);
        
    }
    

    
    private long getUtc(
        Map<String, List<String>> queryMap,
        HttpExchange exchange,
        long errorCode)
            throws IOException {
      
      long utc = HttpServerHelp.requiredLongValue(
          queryMap, Constants.Rest.QS_UTC, exchange, errorCode);
      
      if (utc == errorCode)
        return errorCode;
      
      if (utc < notary.chainParams().inceptionUtc()
          || utc > System.currentTimeMillis() +
          notary.settings().maxCrossMachineTimeSkew()) {
        
        HttpServerHelp.sendBadRequest(
            exchange,
            "Out-of-bounds value in query string: " +
            Constants.Rest.QS_UTC + "=" + utc);
        
        return errorCode;
      }
      
      return utc;
        
    }
    
    
  }   // class UpdateHandler
    
    
  
  
  static BlockProofParser blockProofParser(HashEncoding encoding) {
    switch (encoding) {
    case BASE64_32:
      return BLOCK_PROOF_PARSER_B64;
    case HEX:
      return BLOCK_PROOF_PARSER_HEX;
    default:
      throw new AssertionError("unaccounted encoding: " + encoding);
    }
  }
  
  final static BlockProofParser BLOCK_PROOF_PARSER_B64 =
      new BlockProofParser(HashEncoding.BASE64_32);
  
  final static BlockProofParser BLOCK_PROOF_PARSER_HEX =
      new BlockProofParser(HashEncoding.HEX);
   
  
  
  /** Handler for the "state" URI endpoint. */
  public static class StateHandler extends Base {

    StateHandler(Notary notary, ServerSettings settings) {
      super(notary, settings);
    }
    
    

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var queryMap = HttpServerHelp.queryMap(exchange);
      
      var blockNo =
          HttpServerHelp.optionalLongValue(
              queryMap,
              Constants.Rest.QS_BLOCK,
              exchange);
      
      var encoding =
          getEncoding(queryMap, exchange)
          .orElse(HashEncoding.BASE64_32);
      
      
      
      BlockProof blockProof;
      try {
          blockProof = blockNo.isPresent() ?
              notary.blockProof(blockNo.get()) :
              notary.stateProof();
      } catch (IllegalArgumentException iax) {
        if (blockNo.isPresent()) {
          var msg =
              "requested block [" + blockNo.get() +
              "] is ahead of last committed block no.";
          HttpServerHelp.sendBadRequest(exchange, msg);
        
        } else {
          var msg =
              "cannot fullfill request until block [1] is first committed";
          HttpServerHelp.sendText(exchange, 501, msg);
        }
        return;
      }
      
      var json = blockProofParser(encoding).toJsonObject(blockProof);
      HttpServerHelp.sendJson(exchange, 200, json);
    }
    
  }
  
  

}


























