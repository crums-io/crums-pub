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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import io.crums.util.json.simple.JSONObject;
import io.crums.util.json.simple.parser.JSONParser;
import io.crums.util.json.simple.parser.ParseException;

import io.crums.util.IntegralStrings;
import io.crums.util.Lists;
import io.crums.util.main.Args;
import io.crums.util.main.TablePrint;
import io.crums.model.Constants;
import io.crums.model.CrumRecord;
import io.crums.model.TreeRef;
import io.crums.model.json.CrumTrailParser;
import io.crums.model.json.TreeRefParser;

/**
 * The crums REST client.
 * 
 * <h1>Usage</h1>
 * <p>
 * This following does not require setup:
 * <ul>
 * <li>{@linkplain #getBeacon()}</li>
 * </ul>
 * </p><p>
 * More typically there are hashes to be witnessed, their crumtrails to be retrieved.
 * This a 2 step process. First the hashes are gathered. To set a <em>single hash</em> invoke
 * one of the following:
 * <ul>
 * <li>{@linkplain #setHash(byte[])}</li>
 * <li>{@linkplain #setHash(ByteBuffer)}</li>
 * <li>{@linkplain #setHash(String)}</li>
 * </ul>
 * </p><p>
 * To set <em>multiple</em> hashes invoke any of
 * <ul>
 * <li>{@linkplain #addHash(byte[])}</li>
 * <li>{@linkplain #addHash(ByteBuffer)}</li>
 * <li>{@linkplain #addHash(String)}</li>
 * </ul>
 * for each hash.
 * </p><p>
 * There are 2 choices for retrieving {@linkplain CrumRecord}s for the hashes:
 * <ul>
 * <li>{@linkplain #getCrumRecordsAsJson()}</li>
 * <li>{@linkplain #getCrumRecords()}</li>
 * </ul>
 * Note the above 2 methods do not clear the instance's hashes. To do that
 * <ul>
 * <li>{@linkplain #clearHashes()}</li>
 * </ul>
 * </p>
 * <p>
 * Also note that methods like this that change instance state (i.e. methods with side effects)
 * return the instance itself for invocation chaining.
 * </p>
 * <h2>Not Thread-safe</h2>
 * <p><i>Instances are not thread-safe</i>. To access the server from multiple threads, use one instance per thread.</p>
 * <p>
 * TODO: add list tree-refs
 * </p>
 */
public class Client {
  
  public final static String REST_ROOT_URL = "https://crums.io";

  private HttpClient httpClient;
  
  
  
  
  private final ArrayList<String> hashes = new ArrayList<>();
  
  
  /**
   * @return the hash
   */
  public String getHash() {
    if (hashes.size() > 1)
      throw new IllegalStateException(
          hashes.isEmpty() ? "hash not set" : "ambiguous: " + hashes);
    return hashes.isEmpty() ? null : hashes.get(0);
  }

  /**
   * Sets the query hash to the given single value.
   * 
   * @param hash 64-char hex
   */
  public Client setHash(String hash) {
    hash = hash.trim().toLowerCase();
    checkHash(hash);
    
    hashes.clear();
    hashes.add(hash);
    return this;
  }

  /**
   * Sets the query hash to the given single value.
   * 
   * @param hash 32 bytes
   */
  public Client setHash(byte[] hash) {
    checkHashWidth(hash.length);

    hashes.clear();
    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }


  /**
   * Sets the query hash to the given single value.
   * 
   * @param hash 32 bytes
   */
  public Client setHash(ByteBuffer hash) {
    checkHashWidth(hash.remaining());

    hashes.clear();
    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }


  /**
   * Adds the given hash to the query.
   * 
   * @param hash 64-char hex
   */
  public Client addHash(String hash) {
    hash = hash.trim().toLowerCase();
    checkHash(hash);
    
    hashes.add(hash);
    return this;
  }


  /**
   * Adds the given hash to the query.
   * 
   * @param hash 32 bytes
   */
  public Client addHash(byte[] hash) {
    checkHashWidth(hash.length);

    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }


  /**
   * Adds the given hash to the query.
   * 
   * @param hash 32 bytes
   */
  public Client addHash(ByteBuffer hash) {
    checkHashWidth(hash.remaining());

    hashes.add(IntegralStrings.toHex(hash));
    return this;
  }
  
  
  /**
   * Clears the hashes set or added.
   */
  public Client clearHashes() {
    hashes.clear();
    return this;
  }
  

  /**
   * Returns the added or set hashes.
   * 
   * @return an immutable snapshot of the added hashes
   */
  public List<String> getHashes() {
    return Lists.readOnlyCopy(hashes);
  }
  
  
  
  private void checkHash(String hash) {
    checkHashWidth(IntegralStrings.hexToBytes(hash).length);
  }
  
  private void checkHashWidth(int len) {
    if (len != Constants.HASH_WIDTH)
      throw new IllegalArgumentException("illegal hash width: " + len);
  }
  
  
  
  
  
  
  /**
   * Returns the {@linkplain CrumRecord}s for the added {@linkplain #getHashes() hashes}.
   * 
   * @return non-empty, immutable list of records
   */
  public List<CrumRecord> getCrumRecords() throws ClientException {
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
  
  
  
  /**
   * Returns a hash reference to the latest published tree at the server. Because its hash
   * value cannot be computed in advance, its value acts as a beacon.
   */
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
