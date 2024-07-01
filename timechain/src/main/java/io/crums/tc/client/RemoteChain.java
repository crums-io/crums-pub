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
import io.crums.tc.json.ReceiptParser;
import io.crums.util.json.JsonParsingException;

/**
 * HTTP REST client to a single server.
 */
public class RemoteChain implements NotaryService {


  private final String hostUrl;


  private final HttpClient httpClient;


  
  private HttpClient buildClient() {
    return HttpClient.newBuilder().version(Version.HTTP_1_1).build();
  }


  
  public RemoteChain(String scheme, String host) {
    try {
      URI uri = new URI(scheme.toLowerCase() + "//" + host.toLowerCase());
      checkHostUri(uri);
      this.hostUrl = uri.toString();
    } catch (URISyntaxException usx) {
      throw new IllegalArgumentException(
        "illegal syntax: " + usx.getMessage());
    }
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
    try {
      URI uri = new URI(
        scheme.toLowerCase() + "//" + host.toLowerCase() + ":" + port);
      checkHostUri(uri);
      this.hostUrl = uri.toString();
    } catch (URISyntaxException usx) {
      throw new IllegalArgumentException(
        "illegal syntax: " + usx.getMessage());
    }
    this.httpClient = buildClient();
  }


  public RemoteChain(URI host) {
    this.hostUrl = host.toString();
    checkHostUri(host);
    this.httpClient = buildClient();
  }


  private void checkHostUri(URI host) {
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
    if (host.getRawPath() != null)
      throw new IllegalArgumentException(
        "host URI must not specify a path: " + host);
  }



  @Override
  public NotaryPolicy policy() throws NetworkException {
    // TODO
    return null;
  }


  @Override
  public Receipt witness(ByteBuffer hash) throws NetworkException {
    String url =
        hostUrl + Constants.Rest.WITNESS_URI + '?' +
        Constants.Rest.QS_HASH + '=' +
        HashEncoding.BASE64_32.encode(hash);

    return fetchReceipt(url);
  }



  private Receipt fetchReceipt(String url) throws NetworkException {

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofMinutes(1)).GET().build();

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

      return ReceiptParser.B64.toEntity(body);
      

    } catch (IOException iox) {
      throw new NetworkException(
        "i/o error on attempting " + url + " - cause: " + iox.getMessage(), iox);
    } catch (InterruptedException ix) {
      throw new NetworkException(
        "interrupted on attempting " + url, ix);
    } catch (JsonParsingException jpx) {
      throw new NetworkException(
        "failed on parsing JSON: " + jpx.getMessage(), jpx);
    }
  }

  
  @Override
  public Receipt update(Crum crum) throws NetworkException {
    
    String url =
        hostUrl + Constants.Rest.UPDATE_URI + '?' +
        Constants.Rest.QS_UTC + '=' + crum.utc() + '&' +
        Constants.Rest.QS_HASH + '=' +
        HashEncoding.BASE64_32.encode(crum.hash());

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


    final var surl = url.toString();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(surl))
        .timeout(Duration.ofMinutes(1)).GET().build();

    try {

      HttpResponse<String> response =
          httpClient.send(request, BodyHandlers.ofString());
      
      final var body = response.body();

      observeResponse(surl, body);

      {
        int status = response.statusCode() ;
        
        if (response.statusCode() != 200)
          throw new NetworkException(
            "HTTP status code " + status + " from " + url + "\n" +
            body);
      }



      return BlockProofParser.B64.toEntity(body);
      

    } catch (IOException iox) {
      throw new NetworkException(
        "i/o error on attempting " + url + " - cause: " + iox.getMessage(), iox);
    } catch (InterruptedException ix) {
      throw new NetworkException(
        "interrupted on attempting " + url, ix);
    } catch (JsonParsingException jpx) {
      throw new NetworkException(
        "failed on parsing JSON: " + jpx.getMessage(), jpx);
    }
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





