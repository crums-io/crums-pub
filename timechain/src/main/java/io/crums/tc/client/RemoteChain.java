/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.client;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.time.Duration;
import java.util.Arrays;

import io.crums.sldg.json.HashEncoding;
import io.crums.tc.BlockProof;
import io.crums.tc.Constants;
import io.crums.tc.Crum;
import io.crums.tc.NotaryPolicy;
import io.crums.tc.NotaryService;
import io.crums.tc.Receipt;
import io.crums.tc.except.NetworkException;
import io.crums.tc.json.BlockProofParser;
import io.crums.tc.json.NotaryPolicyParser;
import io.crums.tc.json.ReceiptParser;
import io.crums.util.json.JsonEntityReader;
import io.crums.util.json.JsonParsingException;

/**
 * HTTP REST client to a single server.
 */
public class RemoteChain implements NotaryService, Channel {


  /**
   * Returns the given server address as a normalized URI.
   * 
   * @param scheme  {@code http} or {@code https}
   * @param host    host name
   * 
   * @throws IllegalArgumentException  on malformed / illegal args
   */
  public static URI remoteURI(String scheme, String host) {
    try {
      URI uri = new URI(scheme.toLowerCase() + "//" + host.toLowerCase());
      checkHostUri(uri);
      return uri;
    } catch (URISyntaxException usx) {
      throw new IllegalArgumentException(
        "illegal syntax: " + usx.getMessage());
    }
  }



  public static URI remoteURI(String hostUrl) {
    try {
      URI uri = new URI(hostUrl);
      checkHostUri(uri);
      return uri;
    } catch (URISyntaxException usx) {
      throw new IllegalArgumentException(
        "illegal syntax: " + usx.getMessage());
    }
  }

  /**
   * Returns the given server address as a normalized URI.
   * 
   * @param scheme  {@code http} or {@code https}
   * @param host    host name
   * @param port    positive port number
   * 
   * 
   * @throws IllegalArgumentException  on malformed / illegal args
   */
  public static URI remoteURI(String scheme, String host, int port) {
    try {
      URI uri = new URI(
        scheme.toLowerCase() + "://" + host.toLowerCase() + ":" + port);
      checkHostUri(uri);
      return uri;
    } catch (URISyntaxException usx) {
      throw new IllegalArgumentException(
        "illegal syntax: " + usx.getMessage());
    }
  }

  private volatile boolean open = true;

  private final String hostUrl;


  private final HttpClient httpClient;


  /** Request timeout in seconds. */
  private int timeout = 15;

  private boolean compress;


  
  private HttpClient buildClient() {
    return HttpClient.newBuilder().version(Version.HTTP_1_1).build();
  }


  private RemoteChain(String hostUrl) {
    this.hostUrl = hostUrl;
    this.httpClient = buildClient();
  }
  
  public RemoteChain(String scheme, String host) {
    this.hostUrl = remoteURI(scheme, host).toString();
    this.httpClient = buildClient();
  }










  /**
   * Full constructor.
   * 
   * @param scheme  {@code http} or {@code https}
   * @param host    host name or IP address
   * @param port    positive port number
   */
  public RemoteChain(String scheme, String host, int port) {
    this.hostUrl = remoteURI(scheme, host, port).toString();
    this.httpClient = buildClient();
  }




  public RemoteChain(URI host) {
    this.hostUrl = host.toString().toLowerCase();
    checkHostUri(host);
    this.httpClient = buildClient();
  }



  public static void checkHostUri(URI host) {
    if (!host.isAbsolute())
      throw new IllegalArgumentException(
        "host URI must be absolute: " + host);
    var scheme = host.getScheme().toLowerCase();
    if (!scheme.equals("http") && !scheme.equals("https"))
      throw new IllegalArgumentException(
        "illegal scheme (" + host.getScheme() + "): " + host);
    if (host.getHost() == null)
      throw new IllegalArgumentException(
        "host URI has no host: " + host);
    var path = host.getRawPath();
    if (path != null && !path.isEmpty() && !path.equals("/"))
      throw new IllegalArgumentException(
        "host URI must not specify a path: " + host);
  }


  public URI hostURI() {
    try {
      return new URI(hostUrl);
    } catch (URISyntaxException usx) {
      throw new RuntimeException(
        "assertion failure (should never happen): " + usx.getMessage());
    }
  }

  public RemoteChain defaultCompression(boolean on) {
    compress = on;
    return this;
  }

  public boolean defaultCompression() {
    return compress;
  }


  public int timeout() {
    return timeout;
  }

  public RemoteChain timeout(int seconds) {
    if (seconds <  1)
      throw new IllegalArgumentException(
          "attempt to set timeout to " + seconds + " seconds");
    this.timeout = seconds;
    return this;
  }





  @Override
  public NotaryPolicy policy() throws NetworkException {
    String url = hostUrl + Constants.Rest.POLICY_URI;
    return fetchEntity(url, NotaryPolicyParser.INSTANCE);
  }



  private <T> T fetchEntity(String url, JsonEntityReader<T> parser) {

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeout)).GET().build();

    try {

      HttpResponse<String> response =
          httpClient.send(request, BodyHandlers.ofString());

      final var body = response.body();

      observeResponse(url, body);
      
      {
        int status = response.statusCode() ;
        
        if (status != 200 && status != 202)
          throw new NetworkException(
            "HTTP status code " + status + " from " + url + "\n" +
            body);
      }

      return parser.toEntity(body);
      

    } catch (IOException iox) {
      var msg = "I/O error on attempting " + url;
      if (iox.getMessage() != null)
        msg += " -- Error message: " + iox.getMessage();
      if (!iox.getClass().equals(IOException.class))
        msg += " -- Type: " + iox.getClass().getSimpleName();
      if (iox.getCause() != null && iox.getCause().getMessage() != null)
        msg += " -- Detail: " + iox.getCause().getMessage();
      
      throw new NetworkException(msg, iox);

    } catch (InterruptedException ix) {
      throw new NetworkException(
        "interrupted on attempting " + url, ix);
    } catch (JsonParsingException jpx) {
      throw new NetworkException(
        "failed on parsing JSON: " + jpx.getMessage(), jpx);
    }
  }



  /**
   * Same as {@linkplain #witness(ByteBuffer, long)} interface method, but with
   * the <em>compression</em> option exposed.
   */
  public Receipt witness(
      ByteBuffer hash, long fromBlockNo, boolean compress)
        throws NetworkException {
    
    String url =
        hostUrl + Constants.Rest.WITNESS_URI + '?' +
        Constants.Rest.QS_HASH + '=' +
        HashEncoding.BASE64_32.encode(hash);

    url = appendQs(url, fromBlockNo, compress);

    return fetchReceipt(url);
  }



  private String appendQs(String url, long fromBlockNo, boolean compress) {
    if (fromBlockNo > 1L)
      url += "&" + Constants.Rest.QS_BLOCK + '=' + fromBlockNo;

    else if (fromBlockNo != 1L)
      throw new IllegalArgumentException(
          "out-of-bounds fromBlockNo: " + fromBlockNo);
    
    if (!compress)
      url += "&" + Constants.Rest.COMPRESS + "=0";

    return url;
  }


  @Override
  public Receipt witness(ByteBuffer hash, long fromBlockNo)
      throws NetworkException {

    return witness(hash, fromBlockNo, compress);
  }



  private Receipt fetchReceipt(String url) throws NetworkException {
    return fetchEntity(url, ReceiptParser.B64);
  }

  
  @Override
  public Receipt update(Crum crum, long fromBlockNo) throws NetworkException {
    return update(crum, fromBlockNo, compress);
  }



  public Receipt update(Crum crum, long fromBlockNo, boolean compress) throws NetworkException {

    String url =
        hostUrl + Constants.Rest.UPDATE_URI + '?' +
        Constants.Rest.QS_UTC + '=' + crum.utc() + '&' +
        Constants.Rest.QS_HASH + '=' +
        HashEncoding.BASE64_32.encode(crum.hash());

    url = appendQs(url, fromBlockNo, compress);

    return fetchReceipt(url);
  }

  
  @Override
  public BlockProof stateProof(boolean hi, Long... blockNos)
      throws NetworkException {
    
    if (blockNos.length == 0)
    throw new IllegalArgumentException("empty blockNos");

    var url = new StringBuilder(hostUrl).append(Constants.Rest.STATE_URI);
    
    long blockNo = blockNos[0];
    final boolean canonical = blockNos.length == 1 && blockNo == 1L && hi;
    
    if (!canonical) {

      if (blockNo < 1L)
        throw new IllegalArgumentException(
          "negative block no.: " + Arrays.asList(blockNos));
      url.append('?')
          .append(Constants.Rest.QS_BLOCK).append('=')
          .append(blockNo);
      for (int index = 1; index < blockNos.length; ++index) {
        blockNo = blockNos[index];
        if (blockNo < 1L)
          throw new IllegalArgumentException(
            "negative block no.: " + Arrays.asList(blockNos));
        url.append('&')
            .append(Constants.Rest.QS_BLOCK).append('=')
            .append(blockNo);
      }
      if (!hi)
        url.append('&')
            .append(Constants.Rest.QS_LAST).append('=')
            .append("false");
    }

    if (!compress)
      url.append('&')
          .append(Constants.Rest.COMPRESS)
          .append('=').append('0');


    final var surl = url.toString();

    return fetchEntity(surl, BlockProofParser.B64);
  }


  /** Closes the HTTP client. Exceptions warned on std err; not thrown. */
  @Override
  public void close() {
    this.open = false;
    // try {
    //   // FIXME: the following is *supposed to be in the API, but the
    //   // version loaded in JDK 22 (i.e. sans pom dep declaration) has
    //   // no such method: commented out
    //   this.httpClient.close();
    // } catch (Exception x) {
    //   System.err.println(
    //       "[WARNING] ignoring error on shutting down HTTP client: " +
    //       x.getMessage());
    // }
  } 



  @Override
  public boolean isOpen() {
    return open;
  }



  public RemoteChain reboot() {
    if (isOpen())
      throw new IllegalStateException("instance is still open");
    return new RemoteChain(this.hostUrl);
  }




  /**
   * Subclass hook method for observing the HTTP response. The default
   * is a noop.
   * 
   * @param url       REST GET endpoint including querystring parameters, if any
   * @param body      HTTP response body
   */
  protected void observeResponse(String url, String body) {
  }


}





