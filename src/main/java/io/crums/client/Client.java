/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.client;


import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.crums.util.IntegralStrings;
import io.crums.util.main.Args;
import io.crums.util.main.TablePrint;
import io.crums.model.Constants;
import io.crums.model.CrumRecord;
import io.crums.model.TreeRef;
import io.crums.model.json.CrumTrailParser;
import io.crums.model.json.TreeRefParser;

/**
 * 
 */
public class Client {
  
  public final static String REST_ROOT_URL = "https://crums.io";

  private HttpClient httpClient;
  
  
  
  
  private final ArrayList<String> hashes = new ArrayList<>();
  
  
  /**
   * @return the hash
   */
  public String getHash() {
    if (hashes.size() != 1)
      throw new IllegalStateException(
          hashes.isEmpty() ? "hash not set" : "ambiguous: " + hashes);
    return hashes.get(0);
  }

  /**
   * @param hash the hash to set (in hex)
   */
  public Client setHash(String hash) {
    hash = hash.trim().toLowerCase();
    checkHash(hash);
    
    hashes.clear();
    hashes.add(hash);
    return this;
  }

  /**
   * 
   */
  public Client setHash(byte[] hash) {
    checkHashWidth(hash.length);

    hashes.clear();
    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }

  /**
   * 
   */
  public Client setHash(ByteBuffer hash) {
    checkHashWidth(hash.remaining());

    hashes.clear();
    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }

  /**
   * @param hash the hash to set (in hex)
   */
  public Client addHash(String hash) {
    hash = hash.trim().toLowerCase();
    checkHash(hash);
    
    hashes.add(hash);
    return this;
  }

  /**
   * 
   */
  public Client addHash(byte[] hash) {
    checkHashWidth(hash.length);

    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }

  /**
   * 
   */
  public Client addHash(ByteBuffer hash) {
    checkHashWidth(hash.remaining());

    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }
  
  
  
  public Client clearHashes() {
    hashes.clear();
    return this;
  }
  

  public List<String> getHashes() {
    return Collections.unmodifiableList(new ArrayList<>(hashes));
  }
  
  
  
  private void checkHash(String hash) {
    checkHashWidth(IntegralStrings.hexToBytes(hash).length);
  }
  
  private void checkHashWidth(int len) {
    if (len != Constants.HASH_WIDTH)
      throw new IllegalArgumentException("illegal hash width: " + len);
  }
  
  
  
  
  
  
  /**
   * Returns the {@linkplain CrumRecord}s for the 
   * 
   * @return non-empty, immutable list of records
   */
  public List<CrumRecord> getCrumRecords() {
    return CrumTrailParser.INSTANCE.toCrumRecords( getCrumRecordsAsJson() );
  }
  
  
  
  
  
  
  private final static String STAMP_URL_PREFIX =
      REST_ROOT_URL + Constants.STAMP_PATH + "?" + Constants.QS_HASH_NAME + "=";
  

  
  
  
  /**
   * Returns the server response as a json object.
   * 
   * @return either a <tt>JSONObject</tt> or <tt>JSONArray</tt>
   */
  public Object getCrumRecordsAsJson() throws ClientException {
    if (hashes.isEmpty())
      throw new IllegalStateException("no hashes set");
    
    String url;
    {
      Iterator<String> ih = hashes.iterator();
      StringBuilder str = new StringBuilder(STAMP_URL_PREFIX.length() + 65 * hashes.size() - 1);
      str.append(STAMP_URL_PREFIX).append(ih.next());
      while (ih.hasNext())
        str.append(',').append(ih.next());
      url = str.toString();
    }
    
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofMinutes(1)).GET().build();
    
    
    HttpResponse<String> response;
    
    try {
      response = httpClient().send(request, BodyHandlers.ofString());
      
      if (response.statusCode() == 200 || response.statusCode() == 202)
        return new JSONParser().parse(response.body());

      throw new ClientException(
          response.statusCode() + " HTTP status from " + url + "\n" + response.body());
      
    } catch (IOException iox) {
      throw new ClientException("network error on attempting " + url, iox);
    } catch (InterruptedException ix) {
      throw new ClientException("interrupted on " + url, ix);
    } catch (ParseException px) {
      throw new ClientException("failed to parse response from " + url, px);
    }
  }
  
  
  
  
  public TreeRef getBeacon() throws ClientException {
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(BEACON_URL))
        .timeout(Duration.ofMinutes(1)).GET().build();
    
    try {
      
      HttpResponse<String> response = httpClient().send(request, BodyHandlers.ofString());
      
      if (response.statusCode() != 200)
        throw new ClientException(
            response.statusCode() + " HTTP status from " + BEACON_URL + "\n" + response.body());
      
      JSONObject jtree = (JSONObject) new JSONParser().parse(response.body());
      
      return TreeRefParser.INSTANCE.toTreeRef(jtree);
      
    } catch (IOException iox) {
      throw new ClientException("network error on attempting " + BEACON_URL, iox);
    } catch (InterruptedException ix) {
      throw new ClientException("interrupted", ix);
    } catch (ParseException px) {
      throw new ClientException("failed to parse response", px);
    }
  }
  
  
  private final static String BEACON_URL =
      REST_ROOT_URL + Constants.API_PATH + Constants.BEACON_VERB;
  
  
  
  
  
  private HttpClient httpClient() {
    if (httpClient == null)
      httpClient = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    return httpClient;
  }
  
  
  
  
  
  
  
  public final static String STAMP = "stamp";
  
  // wip
  public static void main(String[] args) throws Exception {
    
    if (Args.help(args)) {
      printHelp();
      return;
    }

    Client client = new Client();

    String hash = Args.getValue(args, STAMP);
    if (hash == null) {
      System.err.println("No commands given.");
      System.err.println();
      printUsage(System.err);
      return;
    }
    
    for (
        StringTokenizer tokenizer = new StringTokenizer(hash, ",");
        tokenizer.hasMoreTokens(); ) {
      
      client.addHash(tokenizer.nextToken());
    }
    
    
    System.out.println();
    System.out.println(client.getCrumRecordsAsJson());
    System.out.println();
  }
  
  
  private static void printHelp() {
    System.out.println();
    System.out.println("Description:");
    System.out.println();
    System.out.println("HTTP REST client for crums.io");
    System.out.println();
    printUsage(System.out);
  }

  
  private static void printUsage(PrintStream out) {
    out.println("Usage:");
    out.println();
    out.println("Arguments are specified as 'name=value' pairs.");
    out.println();
    TablePrint table = new TablePrint(out, 15, 60, 5);
    table.setIndentation(1);
    table.printRow(STAMP + "=*", "1 or more (comma-separated) SHA-256 hashes in hex", "R");
    out.println();
  }

}
