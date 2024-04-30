/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import static io.crums.tc.Constants.HASH_WIDTH;
import static io.crums.tc.notary.NotaryConstants.CARGO_BLOCK_EXT;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.crums.io.FileUtils;
import io.crums.tc.CargoProof;
import io.crums.tc.ChainParams;
import io.crums.tc.Constants;
import io.crums.tc.Crum;
import io.crums.tc.Crumtrail;
import io.crums.tc.TimeBinner;
import io.crums.tc.TimeChain;
import io.crums.tc.except.TimeChainException;
import io.crums.tc.notary.except.NotaryException;
import io.crums.util.Lists;
import io.crums.util.Strings;

/**
 * 
 */
public class CargoChain implements Channel {

  
  public final static TimeBinner FINEST_BINNER = TimeBinner.MILLIS_32;
  public final static int MIN_BINNER_EXP = FINEST_BINNER.binExponent();
  
  
  /**
   * @see CargoChain#CargoChain(InitArgs)
   */
  public record InitArgs(
      TimeChain timechain,
      NotarySettings settings,
      File dir,
      NotaryLog log,
      NotaryLog blockLog) {
    
    public InitArgs {
      if (!timechain.params().equals(settings.chainParams()))
        throw new IllegalArgumentException(
            "timechain / settings mismatch: " +
            timechain.params() + " / " + settings.chainParams());
      if (timechain.params().timeBinner().binExponent() < MIN_BINNER_EXP)
        throw new IllegalArgumentException(
            "timechain block duration ambitiously too short: " +
            timechain.params().timeBinner() + " (exponent: " +
            timechain.params().timeBinner().binExponent() +
            "); minimum is " + TimeBinner.forExponent(MIN_BINNER_EXP) +
            " (exponent: " + MIN_BINNER_EXP + ")");
      
      Objects.requireNonNull(dir);
      Objects.requireNonNull(log);
      Objects.requireNonNull(blockLog);
      
      if (!timechain.isOpen())
        throw new IllegalArgumentException(
            "chain is closed: " + timechain);
    }
    
    public InitArgs(
        TimeChain timechain,
        NotarySettings policy,
        File dir,
        NotaryLog log) {
      this(timechain, policy, dir, log, log);
    }
  }
  
  
  protected final static FilenameFilter CARGO_BLOCK_FILTER =
      new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(CARGO_BLOCK_EXT);
        }
      };
      
  
  

      
      
      
      
      
      
      
      
      
      
      
  private final TimeChain timechain;
  private final NotarySettings settings;
  private final ChainParams chainParams;
  private final File dir;
  private final NotaryLog log;
  private final NotaryLog blockLog;

  /**
   * @see InitArgs
   */
  public CargoChain(InitArgs args) {
    this.timechain = args.timechain();
    this.settings = args.settings();
    this.chainParams = timechain.params();
    this.dir = args.dir();
    try {
      FileUtils.ensureDir(dir);
    } catch (Exception x) {
      var nx = new NotaryException(
          "on  <init>(): " + x.getMessage(),
          x);
      args.log().fatal(nx);
      throw nx;
    }
    this.log = args.log();
    this.blockLog = args.blockLog();
  }
  
  
  
  
  
  
  public TimeChain timechain() {
    return timechain;
  }
  
  
  
  public NotarySettings settings() {
    return settings;
  }
  
  
  
  @Override
  public boolean isOpen() {
    return timechain.isOpen();
  }



  @Override
  public void close() {
    timechain.close();
  }




  private final static int CB_EXT_LEN = CARGO_BLOCK_EXT.length();
  
  
  
  protected record BlockDir(long blockNo, String dirname)
      implements Comparable<BlockDir> {
    
    /**
     * {@code blockNo} is inferred from the {@code dir}
     * name.
     */
    protected BlockDir(String dirname) throws NumberFormatException {
      this(
          Long.parseLong(
              dirname.substring(
                  0, dirname.length() - CB_EXT_LEN)), dirname);
    }

    @Override
    public int compareTo(BlockDir o) {
      long diff = blockNo - o.blockNo;
      return diff < 0 ? -1 : (diff == 0 ? 0 : 1);
    }
    
    public File toFile(File dir) {
      return new File(dir, dirname);
    }
  }
  
  
  public Receipt addCrum(FreshCrum crum) {
    Crum out = getBlockForWrite(crum).addCrum(crum);
    return new Receipt(chainParams, out);
  }
  
  
  
  protected final CargoBlock getBlockForWrite(FreshCrum crum) {
    
    final long crumBlockNo = chainParams.blockNoForUtc(crum.utc());
    var blocks = activeBlocksLazy();
    
    if (blocks.isEmpty())
      return createNewBlock(crumBlockNo);
    

    final CargoBlock last = blocks.get(blocks.size() - 1);
    final long diff = crumBlockNo - last.blockNo();
    
    if (diff == 0)
      return last;
    
    if (diff == 1) 
      return createNewBlock(crumBlockNo);
      
    if (diff > 1) {
      log.info(
          "skipping " + Strings.nOf(diff - 1, "cargo block") +
          " (to block [" + crumBlockNo + "])");
      return createNewBlock(crumBlockNo);
    }
    
    
    if (diff == -1) {
      
      // this can be an edge case race condition when
      // multiple threads of execution(1) are accessing
      // (the file system). Usually, that's OK..
      
      // but make sure the current "thread"(2) has not "paused" 
      // for too long (since when the FreshCrum was 1st created)
      //
      // (1) threads or processes
      // (2) in quotes cuz it's the same w/ virtual threads
      //
      final long now = System.currentTimeMillis();
      final long utcDiff = now - crum.utc();
      if (utcDiff > settings.maxConcurrentLag()) {
        
        int maxLag = settings.maxConcurrentLag();
        var error = new AssertionError(
            "maxConcurrentLag (" + maxLag +
            ") breached by " + (utcDiff - maxLag) + " millis: " +
            crum);
        log.fatal(error);
        throw error;
      }
      
      // check for cross machine time skews..
      
      final long blockTimeSkew = now -
          settings.chainParams().utcForBlockNo(crumBlockNo + 1);
      
      if (blockTimeSkew < 0 &&
          -blockTimeSkew > settings.maxCrossMachineTimeSkew()) {
        
        var error = new AssertionError(
            "Cross-machine time skew detected: block [" + (crumBlockNo + 1) +
            "] is ahead of local system time by " + blockTimeSkew +
            " milliseconds; maximum allowed is " +
            settings.maxCrossMachineTimeSkew());
        
        log.fatal(error);
        throw error;
      }
      
      // alright, we'll use the cargo block 2nd from the 
      // frontier block (whether one exists or not):
      // remember, the blocks are firstly conceptual (model),
      // and then secondly, manifested as directories
      
      int count = blocks.size();
      if (count > 1) {
        var nextToLast = blocks.get(count - 1);
        return nextToLast.blockNo() == crumBlockNo ?
            nextToLast : createNewBlock(crumBlockNo);
      }
      
    } // if (diff == -1)
    
    // diff < -1
    
    var e = new AssertionError(
        "tail block [" + (crumBlockNo - diff) + "] ahead of " +
        crum + " block [" + crumBlockNo + "] by " + -diff + " blocks");
    
    log.fatal(e);
    throw e;
    
  }
  
  
  private File blockDirFile(long blockNo) {
    return new File(dir, Long.toString(blockNo) + CARGO_BLOCK_EXT);
  }
  
  
  private CargoBlock createNewBlock(long blockNo) {
    return createNewBlock(blockNo, false);
  }
  
  
  private CargoBlock getBlockIfPresent(long blockNo) {
    return createNewBlock(blockNo, true);
  }
  
  
  /**
   * Creates and returns a new {@code CargoBlock} instance.
   * 
   * @param blockNo   &ge; 1
   * @param readOnly  if {@code true}, and the block directory
   *                  does not exist, then {@code null} is returned
   *                  
   * @return not {@code null}, if {@code readOnly} is {@code false}
   */
  protected CargoBlock createNewBlock(long blockNo, boolean readOnly) {
    File cbDir = blockDirFile(blockNo);
    if (readOnly && !cbDir.exists())
      return null;
    
    var args = new CargoBlock.InitArgs(
        settings, blockNo, cbDir, blockLog, readOnly);
    return new CargoBlock(args);
  }
  
  
  public Optional<Receipt> findReceipt(ByteBuffer hash) {
    
    if (hash.remaining() != HASH_WIDTH)
      throw new IllegalArgumentException("wrong hash width: " + hash);
    
    return findReceipt(hash, activeBlocksLazy());
  }
  
  
  private Optional<Receipt> findReceipt(
      ByteBuffer hash, List<CargoBlock> blocks) {
    
    final long commitNo = timechain.size();
    
    for (int index = blocks.size(); index-- > 0; ) {
      
      CargoBlock  block = blocks.get(index);
      
      var receipt = findReceipt(hash, block, commitNo);
      if (receipt != null)
        return Optional.of(receipt);
      
    } // for
    
    return Optional.empty();
  }
  
  
  /**
   * Returns a receipt from the given {@code block}, if any; {@code null}, o.w.
   * 
   * @return  {@code null}, if not found
   */
  private Receipt findReceipt(
      ByteBuffer hash, CargoBlock block, long commitNo) {
    
    if (block.blockNo() <= commitNo) {
      
      CargoProof cargoProof = block.findCargoProof(hash);
      if (cargoProof != null) {
        var blockProof = timechain.getBlockProof(block.blockNo());
        var crumtrail = Crumtrail.newMerkleTrail(blockProof, cargoProof);
        return new Receipt(crumtrail);
        
      }
      
      Crum crum = block.findLoneCommit();
      if (crum == null)
        throw new NotaryException(
            "expected commit file not found cargo block [" + block.blockNo() +
            "]; chain commit at [" + commitNo + "]");
      
      if (crum.hash().equals(hash)) {
        var blockProof = timechain.getBlockProof(block.blockNo());
        var crumtrail = Crumtrail.newLoneTrail(blockProof, crum);
        return new Receipt(crumtrail);
      }
      
      return null;
    }
    
    Crum crum = block.findHexTreeCrum(hash);
    return crum == null ? null : new Receipt(chainParams, crum);
  }
  
  
  
  public Optional<Receipt> findReceipt(Crum hintCrum) {
    
    final long blockNo = chainParams.blockNoForUtcUnchecked(hintCrum.utc());
    if (blockNo <= 0)
      return findReceipt(hintCrum.hash());
    
    final long commitNo = timechain.size();
    
    if (blockNo > commitNo)
      return findReceipt(hintCrum.hash());
    
    var cargoBlock = getBlockIfPresent(blockNo);
    if (cargoBlock != null) {
      var rcpt = findReceipt(hintCrum.hash(), cargoBlock, commitNo);
      if (rcpt != null)
        return Optional.of(rcpt);
    }
    
    return findReceipt(hintCrum.hash());
  }
  
  
  

  
  
  
  public List<CargoBlock> activeBlocks() {
    var blockDirs = activeBlockDirs();
    final int count = blockDirs.size();
    var array = new CargoBlock[count];
    for (int index = count; index-- > 0; ) {
      var bDir = blockDirs.get(index);
      array[index] = toCargoBlock(bDir);
    }
    return Lists.asReadOnlyList(array);
  }
  
  
  
  public int build() {
    try {
      // note the current "commit" block no.
      final long lastCommitNo = timechain.size();
      
      final long now = System.currentTimeMillis();
      
      var bds = sortedBlockDirs();
      final int startIndex; // (inc)
      final int endIndex;   // (exc)
      {
        // find the start index..
        final int cindex = Collections.binarySearch(
            Lists.map(bds, BlockDir::blockNo),
            lastCommitNo);
        
        if (cindex < 0) {
          int insertIndex = -1 - cindex;
          if (insertIndex == bds.size())  // (true if bds.isEmpty)
            return 0;
          startIndex = insertIndex;
        } else {
          startIndex = cindex + 1;
        }
        
        // find the end index..
        final long commitMillisPad =
            settings.blockCommitLag() + 1;

        int iend = bds.size();
        for (int index = iend; index-- > 0; ) { // at least once
          long nextBlockUtc =
              chainParams.utcForBlockNo(
                  bds.get(index).blockNo() + 1);
          if (nextBlockUtc + commitMillisPad <= now)
            break;
          iend = index;
        }
        endIndex = iend;
      }
      
      if (startIndex == endIndex)
        return 0;
      
      var buildDirs = bds.subList(startIndex, endIndex);
      int tally = 0;
      for (var bd : buildDirs) {
        var block = toCargoBlock(bd);
        final int crumsAdded = block.buildCargo();
        tally += crumsAdded;
        var chash = block.cargoHash();
        final long blockNo = block.blockNo();
        assertCargoHashOnBuild(chash, blockNo, crumsAdded);
        timechain.recordBlockNo(blockNo, chash);
        log.info(
            "block [" + blockNo + "] committed (" +
            Strings.nOf(crumsAdded, "crum") + ")");
      }
      
      return tally;
      
    } catch (TimeChainException tcx) {
      throw tcx;
    } catch (Exception x) {
      throw new NotaryException(x);
    }
  }
  
  
  
  
  
  /** Used only for equality tests. */
  private final static ByteBuffer SH = Constants.DIGEST.sentinelHash();
  
  private void assertCargoHashOnBuild(
      ByteBuffer chash, long blockNo, int crumsAdded) {
    
    if (chash == null) {
      var error = new AssertionError(
          "null cargo hash for block [" + blockNo+
          "] on build");
      
      log.fatal(error);
      throw error;
    }
    
    if (chash.equals(SH) && crumsAdded != 0) {
      var error = new AssertionError(
          "sentinel cargo hash for block [" + blockNo+
          "] but " + Strings.nOf(crumsAdded, "crum") +
          " reportedly added on build");
      
      log.fatal(error);
      throw error;
    }
    
  }
  
  
  
  
  
  protected final List<CargoBlock> activeBlocksLazy() {
    return Lists.map(activeBlockDirs(), this::toCargoBlock);
  }
  
  
  
  
  /**
   * Returns the existing cargo block directory names.
   * 
   * @return unsort, non-null array
   */
  protected final String[] listCargoDirNames() {
    var names = dir.list(CARGO_BLOCK_FILTER);
    if (names == null)
      throw new NotaryException(
          "failed to list files in cargo chain directory " +
          dir.getAbsolutePath());
    return names;
  }
  
  
  /**
   * Returns the existing cargo block directories as a sorted
   * list of {@link BlockDir}s. The returned list is <em>not</em>
   * lazily loaded.
   */
  protected final List<BlockDir> sortedBlockDirs() throws NumberFormatException {
    String[] cds = listCargoDirNames();
    BlockDir[] bds = new BlockDir[cds.length];
    for (int index = cds.length; index-- > 0; )
      bds[index] = new BlockDir(cds[index]);
    Arrays.sort(bds);
    return Lists.asReadOnlyList(bds);
  }
  
  
  /**
   * Returns the tail end of the {@link #sortedBlockDirs()} list,
   * no greater in size than the {@link NotaryPolicy#blocksRetained()}
   * setting.
   */
  protected final List<BlockDir> activeBlockDirs() {
    var all = sortedBlockDirs();
    int size = all.size();
    int maxBlocks = settings.blocksRetained();
    return size > maxBlocks ?
        all.subList(size - maxBlocks, size) : all;
  }
  
  
  
  protected final CargoBlock toCargoBlock(BlockDir bDir) {
    var args = new CargoBlock.InitArgs(
        settings, bDir.blockNo(), bDir.toFile(dir), blockLog);
    return new CargoBlock(args);
  }
  

}




















