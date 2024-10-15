/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.client;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;

import io.crums.tc.Crum;
import io.crums.tc.Crumtrail;
import io.crums.tc.NotaryPolicy;
import io.crums.tc.Receipt;
import io.crums.tc.client.Repo.ChainRepo;

/**
 * Repo-aware timechain client. Repos are finnicky with respect
 * to the crumtrails they record. That's because each recorded
 * crumtrail is designed to also update (extend) the block proofs of 
 * previous crumtrails recorded for that chain. This class, then,
 * manages and calls {@linkplain RemoteChain} instances and hides some of
 * the implementation-specifics of the {@linkplain Repo} class that
 * users will likely not be interested in.
 * 
 * 
 * @see RemoteChain
 * @see Repo
 * @see TrailRepo
 */
public class Client implements AutoCloseable {

  private final Map<String, RemoteChain> remotes = new TreeMap<>();

  protected final Object lock() { return remotes; }

  private final Repo repo;



  /**
   * Creates an instance using the user-default repo.
   * 
   * @see Repo#DEFAULT_NAME
   * @see Repo#userRepo()
   */
  public Client() {
    this(Repo.userRepo());
  }





  public Client(Repo repo) {
    this.repo = Objects.requireNonNull(repo);
  }



  public Optional<URI> getDefaultHostURI() {
    return repo.defaultChainRepo().map(ChainRepo::origin);
  }


  public Optional<String> getDefaultHost() {
    return repo.getDefaultHost();
  }



  /**
   * Sets the default Timechain host.
   * 
   * @param host    <em>existing</em> chain repo's hostname
   */
  public Client setDefaultHost(String host) {
    repo.setDefaultHost(host);
    return this;
  }






  /** Used in methods with sans-<em>host</em> argument in signatures. */
  private String defaultHost() {
    return repo.getDefaultHost().orElseThrow(
      () -> new IllegalStateException("default host not set"));
  }



  public File repoDir() {
    return repo.rootDir();
  }



  public NotaryPolicy init(URI hostUri) {

    var remote = getRemote(hostUri);
    return repo.getChainRepo(remote).policy();
  }



  public Optional<NotaryPolicy> getPolicy(String host) {
    return repo.findChainRepo(host).map(ChainRepo::policy);
  }


  


  public Receipt witness(ByteBuffer hash) {
    return witness(hash, defaultHost());
  }


  @SuppressWarnings("resource")
  public Receipt witness(ByteBuffer hash, String host) {
    return witup(hash, hash, host, getRemoteOrThrow(host)::witness);

  }

  public Receipt update(ByteBuffer hash) {
    return update(hash, defaultHost());
  }

  public Receipt update(ByteBuffer hash, String host) {
    var notes = repo.listPending(hash, host);
    return notes.isEmpty() ?
        witness(hash, host) :
        update(notes.get(0).crum(), host);
  }


  @SuppressWarnings("resource")
  public Receipt update(Crum crum, String host) {
    return witup(crum, crum.hash(), host, getRemoteOrThrow(host)::update);
  }



  /**
   * Finds and returns an archived crumtrail with the given {@code hash} from
   * the chain repo with the given hostname. The returned proof, if any,
   * will not assert the chain's lineage back to the genesis block.
   * 
   * @param hash        32-bytes
   * @param host        hostname for <em>existing</em> chain repo
   * 
   * @return {@code findTrail(hash, host, false)}
   * 
   * @throws IllegalArgumentException if {@code host} is unknown to this repo
   * @see #findTrail(ByteBuffer, String, boolean)
   */
  public Optional<Crumtrail> findTrail(ByteBuffer hash, String host) {
    return findTrail(hash, host, false);
  }



  /**
   * Finds and returns a crumtrail with the given {@code hash} from the chain
   * repo with the given hostname.
   * 
   * @param hash        32-bytes
   * @param host        hostname for <em>existing</em> chain repo
   * @param incLineage  if {@code true}, then the returned crumtrail (if any)
   *                    contains a hashproof starting from the chain's genesis
   *                    block
   * 
   * @throws IllegalArgumentException if {@code host} is unknown to this repo
   */
  public Optional<Crumtrail> findTrail(
      ByteBuffer hash, String host, boolean incLineage) {
    
    return
        repo.findChainRepo(host)
        .map(ChainRepo::trails)
        .orElseThrow(
            () -> new IllegalArgumentException("unkown host: " + host))
        .findTrail(hash, incLineage);
  }

/**
   * Finds and returns crumtrails from <em>all</em> hosts (chains)
   * this repo knows about. The returned trails, if any, will only
   * contain forward lineage information; they will <em>not</em>
   * contain lineage info back to their resp. chains' genesis blocks.
   * 
   * @param hash        32-bytes
   * 
   * @return {@code findTrails(hash, false)}
   * 
   * @see #findTrails(ByteBuffer, boolean)
   */
  public List<Repo.HostedTrail> findTrails(ByteBuffer hash) {
    return findTrails(hash, false);
  }


  /**
   * Finds and returns crumtrails from <em>all</em> hosts (chains)
   * this repo knows about.
   * 
   * @param hash        32-bytes
   * @param incLineage  if {@code true}, then each returned crumtrail (if any)
   *                    contains a hashproof starting from its chain's genesis
   *                    block
   * 
   * @return  immutable, possibly empty list
   */
  public List<Repo.HostedTrail> findTrails(
      ByteBuffer hash, boolean incLineage) {
    
    return repo.findTrails(hash, incLineage);
  }







  private <T> Receipt witup(
      T obj, ByteBuffer hash, String host,
      BiFunction<T, Long, Receipt> func) {
        
    
    var remote = getRemoteOrThrow(host);

    var trailRepo = repo.getChainRepo(remote).trails();

    var existingTrail = trailRepo.findTrail(hash);
    
    if (existingTrail.isPresent())
      return new Receipt(existingTrail.get());


    long fromBlockNo = Math.max(1L, trailRepo.blockNo());
    
    var rcpt = func.apply(obj, fromBlockNo);
    
    if (rcpt.hasTrail()) {
      trailRepo.add(rcpt.trail());
      repo.removePending(rcpt.crum(), host);
    } else
      repo.addPending(rcpt.crum(), host);
    
    return rcpt;
  }











  @Override
  public void close() {
    synchronized (lock()) {
      remotes.values().forEach(RemoteChain::close);
    }
  }




  private RemoteChain getRemoteOrThrow(String host) {
    return getRemote(host).orElseThrow(
        () -> new IllegalArgumentException(
            "host URI not initialized for hostname: " + host));
  }

  protected final Optional<RemoteChain> getRemote(String host) {
    synchronized (lock()) {
      var remote = this.remotes.get(host);
      
      return remote != null ?
          Optional.of(ensureOpen(remote)) :
          repo.findChainRepo(host).map(ChainRepo::origin).map(this::getRemote);
    }
  }


  private RemoteChain ensureOpen(RemoteChain remote) {
    if (remote.isOpen())
      return remote;
    synchronized (lock()) {
      remote = remote.reboot();
      this.remotes.put(remote.hostURI().getHost(), remote);
      return remote;
    }
  }


  protected final RemoteChain getRemote(URI hostUri) {
    synchronized (lock()) {
      var host = hostUri.getHost();
      var remote = this.remotes.get(host);

      if (remote == null) {
        remote = new RemoteChain(hostUri);
        this.remotes.put(host, remote);
        return remote;
      }
      if (!remote.hostURI().equals(hostUri))
        throw new IllegalArgumentException(
            "multiple URIs to same host -- existing: " + remote.hostURI() +
            ", argument: " + hostUri);

      return ensureOpen(remote);
    }
  }

}
