/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import io.crums.tc.ChainParams;
import io.crums.tc.NotaryPolicy;
import io.crums.util.TidyProperties;

/**
 * Notary settings. The parts of the settings an end user might
 * be interested (and which therefore might be made public)
 * are gathered in the base class {@linkplain NotaryPolicy}.
 */
public class NotarySettings extends NotaryPolicy {
  
  public final static int DEFAULT_MAX_CROSS_MACHINE_TIME_SKEW = 1024;

  /**
   * Returns the maximum value {@link #maxConcurrentLag()} can
   * take. (Didn't want to name it {@code maxMaxConc..}). It's
   * also the value overloaded constructors default to.
   * 
   * @param chainParams  the bin duration determines return value
   * @return {@code chainParams.timeBinner().duration() / 2}
   */
  public static int maxConcurrentLag(ChainParams chainParams) {
    return chainParams.timeBinner().duration() / 2;
  }

  private final int maxConcurrentLag;
  private final int maxCrossMachineTimeSkew;
  
  

  /** Constructs an instance with reasonable defaults. */
  public NotarySettings(ChainParams params, int blocksRetained) {
    super(params, blocksRetained);
    this.maxConcurrentLag = maxConcurrentLag(params);
    this.maxCrossMachineTimeSkew =
        DEFAULT_MAX_CROSS_MACHINE_TIME_SKEW;
  }
  
  

  /** Constructs an instance with reasonable defaults. */
  public NotarySettings(
      ChainParams params, int blocksRetained, int blocksSearched) {
    super(params, blocksRetained, blocksSearched);
    this.maxConcurrentLag = maxConcurrentLag(params);
    this.maxCrossMachineTimeSkew =
        DEFAULT_MAX_CROSS_MACHINE_TIME_SKEW;
  }
  
  /** Constructs an instance with reasonable defaults. */
  public NotarySettings(NotaryPolicy policy) {
    super(policy);
    this.maxConcurrentLag = maxConcurrentLag(policy.chainParams());
    this.maxCrossMachineTimeSkew =
        DEFAULT_MAX_CROSS_MACHINE_TIME_SKEW;
  }
  
  /**
   * Full constructor.
   * 
   * @param policy                  base settings (public)
   * @param maxConcurrentLag        see {@link #maxConcurrentLag()}
   * @param maxCrossMachineTimeSkew see {@link #maxCrossMachineTimeSkew()}
   */
  public NotarySettings(
      NotaryPolicy policy,
      int maxConcurrentLag,
      int maxCrossMachineTimeSkew) {
    
    super(policy);
    this.maxConcurrentLag = maxConcurrentLag;
    this.maxCrossMachineTimeSkew = maxCrossMachineTimeSkew;
    
    if (maxConcurrentLag > maxConcurrentLag(chainParams()))
      throw new IllegalArgumentException(
          "maxConcurrentLag (" + maxConcurrentLag +
          ") must be at least 1/2 of block duration (" +
          chainParams().timeBinner().duration() +
          ")");
    
    if (maxCrossMachineTimeSkew <= 0)
      throw new IllegalArgumentException(
          "negative maxCrossMachineTimeSkew: " + maxCrossMachineTimeSkew);
          
  }
  
  
  protected NotarySettings(NotarySettings copy) {
    super(copy);
    this.maxConcurrentLag = copy.maxConcurrentLag;
    this.maxCrossMachineTimeSkew = copy.maxCrossMachineTimeSkew;
  }
  
  

  /**
   * Maximum time (millis) allowed for a newly seen hash to cross certain
   * critical sections of code. For the most part, this amounts to defining
   * maximum duration a {@link FreshCrum} is considered valid on input.
   * <p>
   * The returned value is no greater than half [time] bin duration.
   * </p>
   * <h4>Warning</h4>
   * <p>
   * <em>Setting this to too low a value may cause the VM to exit on an
   * assertion error.</em> (But that should happen only under load, which
   * is the point of this.)
   * </p>
   */
  public final int maxConcurrentLag() {
    return maxConcurrentLag;
  }

  /**
   * Maximum clock skew (millis) across machines. Along certain execution
   * paths, it's possible to discover (we hope not to) objects that were
   * written ahead of system time. When so (again, hope never to encounter),
   * the instance that detected the skew attempts to stop the notary
   * instance by throwing an assertion error. This setting determines
   * when to panic.
   */
  public final int maxCrossMachineTimeSkew() {
    return maxCrossMachineTimeSkew;
  }
  
  
  
  
  

  /** Equality sans {@code Object.equals(..)} formalities. */
  public final boolean equalSettings(NotarySettings other) {
    return this == other ||
        equalPolicy(other) &&
        maxConcurrentLag == other.maxConcurrentLag &&
        maxCrossMachineTimeSkew == other.maxCrossMachineTimeSkew;
  }
  
  
  
  
  
  
  
  /** Property names used in properties file. */
  public static class PropNames {
    
    public final static String ROOT = "notary.";

    
    public final static String BLOCK_COMMIT_LAG =
        ROOT + "blockCommitLag";
    
    public final static String BLOCKS_RETAINED =
        ROOT + "blocksRetained";
    
    public final static String BLOCKS_SEARCHED =
        ROOT + "blocksSearched";
    
    public final static String MAX_CONCURRENT_LAG =
        ROOT + "maxConcurrentLag";
    public final static String MAX_CROSS_MACHINE_TIME_SKEW =
        ROOT + "maxCrossMachineTimeSkew";
    
    
    public final static List<String> inOrder() {
      return List.of(
          BLOCK_COMMIT_LAG,
          BLOCKS_RETAINED,
          BLOCKS_SEARCHED,
          MAX_CONCURRENT_LAG,
          MAX_CROSS_MACHINE_TIME_SKEW);
    }
    private PropNames() {  }
  }
  
  

  /**
   * Returns the instance properties, sans the chain params
   * (maintained by the chain itself).
   * 
   * @see #load(Properties, ChainParams)
   */
  public Properties toProperties() {
    var props = new TidyProperties(orderedPropNames());

    props.put(
        PropNames.BLOCK_COMMIT_LAG,
        Integer.toString(blockCommitLag()));
    props.put(
        PropNames.BLOCKS_RETAINED,
        Integer.toString(blocksRetained()));
    props.put(
        PropNames.BLOCKS_SEARCHED,
        Integer.toString(blocksSearched()));
    props.put(
        PropNames.MAX_CONCURRENT_LAG,
        Integer.toString(maxConcurrentLag()));
    props.put(
        PropNames.MAX_CROSS_MACHINE_TIME_SKEW,
        Integer.toString(maxCrossMachineTimeSkew()));
    
    return props;
  }
  
  
  
  protected List<String> orderedPropNames() {
    return PropNames.inOrder();
  }
  
  
  
  
  
  /**
   * @see #load(Properties, ChainParams)
   * @see #toProperties()
   */
  public static NotarySettings load(
      File propsFile, ChainParams chainParams)
          throws IOException {
    
    Objects.requireNonNull(chainParams, "null chainParams");
    
    var props = new Properties();
    try (var reader = new FileReader(propsFile)) {
      props.load(reader);
    }
    
    return load(props, chainParams);
  }
  
  
  

  /**
   * Loads the settings using information from both the given
   * {@code props} and {@code chainParams}.
   * 
   * @see #load(File, ChainParams)
   * @see #toProperties()
   */
  public static NotarySettings load(
      Properties props, ChainParams chainParams) {
    
    int blockCommitLag = getIntProperty(props, PropNames.BLOCK_COMMIT_LAG);
    int blocksRetained = getIntProperty(props, PropNames.BLOCKS_RETAINED);
    int blocksSearched = getIntProperty(props, PropNames.BLOCKS_SEARCHED);
    int maxConcurrentLag = getIntProperty(props, PropNames.MAX_CONCURRENT_LAG);
    int maxCrossMachineTimeSkew =
        getIntProperty(props, PropNames.MAX_CROSS_MACHINE_TIME_SKEW);
    
    var policy = new NotaryPolicy(chainParams, blocksRetained, blockCommitLag, blocksSearched);
    return new NotarySettings(
        policy, maxConcurrentLag, maxCrossMachineTimeSkew);
  }
  
  
  private static int getIntProperty(Properties props, String name) {
    String sval = props.getProperty(name);
    if (sval == null)
      throw new IllegalArgumentException(
          "missing required property <" + name + ">");
    try {
      return Integer.parseInt(sval);
    
    } catch (NumberFormatException nfx) {
      throw new NumberFormatException(name + ": " + sval);
    }
  }
  
  
  
}




















