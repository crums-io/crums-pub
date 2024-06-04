/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.client;


import static io.crums.tc.json.JsonTags.BLOCK_PROOF;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import io.crums.io.FileUtils;
import io.crums.io.Opening;
import io.crums.io.SerialFormatException;
import io.crums.io.channels.ChannelUtils;
import io.crums.sldg.HashConflictException;
import io.crums.stowkwik.io.HexPathTree;
import io.crums.tc.BlockProof;
import io.crums.tc.Crum;
import io.crums.tc.Crumtrail;
import io.crums.util.IntegralStrings;
import io.crums.util.Lists;
import io.crums.util.RandomId;

/**
 * Local repo for crumtrails from a <em>single</em> timechain. To the
 * degree feasible, this aims to be resilient against concurrent writers
 * so that it'd be suitable to access based on a user's home-directory,
 * for e.g.
 * <p>
 * Note to self: don't be too cute with optimizations; we're on the
 * client side and we're i/o bound in so many places, it's hard to
 * know which ones matter ahead of testing.
 * </p>
 */
public class TrailRepo implements AutoCloseable {

  public final static int DEFAULT_TBN_MAX_LOOKBACK = 3;

  public final static String PURGED_EXT = ".purg";
  public final static String CARGO_PROOF_EXT = ".ccp";
  public final static String BLOCK_PROOF_EXT = ".tbp";
  public final static String TBN_FILE = "TBN";
  public final static String MAIN_STATE_PREFIX = "main";


  /** Staging directory name. */
  public final static String STAGING = "staging";

  private final Object tbnLock = new Object();

  protected final File dir;
  protected final String chainExt;
  protected final HexPathTree trailTree;
  protected final File stagingDir;

  private FileChannel tbn;

  protected final FileChannel tbn() {
    synchronized (tbnLock) {
      if (tbn == null) {
        try {
          tbn = Opening.CREATE_ON_DEMAND.openChannel(tbnFile());
        } catch (IOException iox) {
          throw new UncheckedIOException(iox);
        }
      }
      return tbn;
    }
  }

  protected final void closeTbn() {
    synchronized (tbnLock) {
      if (tbn == null)
        return;
      
      try {
        tbn.close();
      } catch (IOException gulp) { }
      tbn = null;
    }
  }




  

  public TrailRepo(File dir) {
    this(dir, dir, BLOCK_PROOF_EXT, CARGO_PROOF_EXT);
  }


  protected TrailRepo(
      File dir, File cargoDir, String chainExt, String cargoExt) {
    this.dir = dir;
    this.chainExt = chainExt;
    this.stagingDir = FileUtils.ensureDir(new File(dir, STAGING));
    this.trailTree = new HexPathTree(cargoDir, cargoExt);
  }


  @Override
  public void close() {
    closeTbn();
  }



  protected boolean deleteBlockProof(File file) {
    if (!file.getName().endsWith(BLOCK_PROOF))
      throw new IllegalArgumentException(
          "expected " + BLOCK_PROOF + " file; actual given: " + file);
    
    return delete(file);
  }

  private final static int PURGE_MOVE_ATTEMPTS = 2;


  private boolean delete(File file) {
    int attemptsRemaining = PURGE_MOVE_ATTEMPTS;
    File target = purgeFilepath(file);
    while (file.exists() && attemptsRemaining-- > 0)
      if (file.renameTo(target))
        break;
    
    // if it moved, then it's logically deleted
    final boolean moved = attemptsRemaining >= 0;
    if (moved)
      target.delete();  // attempt an actual delete
    
    return moved;
  }

  private final static int MAX_NUMBERED_PURGE = 16;

  private File purgeFilepath(File file) {
    for (int index = 0; index < MAX_NUMBERED_PURGE; ++index) {
      File target = purgeFilepath(file, index);
      if (!target.exists())
        return target;
    }
    throw new IllegalStateException(
      "too many " + PURGED_EXT + " files for " + file);
  }

  private File purgeFilepath(File file, int index) {
    String prefix = file.getName();
    if (index > 0)
      prefix += "." + index;
    return new File(file.getParentFile(), prefix + PURGED_EXT);
  }



  protected boolean recordTargetBlockNo(long blockNo) throws IOException {
    return recordTargetBlockNo(blockNo, DEFAULT_TBN_MAX_LOOKBACK);
  }


  protected boolean recordTargetBlockNo(long blockNo, int maxLookback)
      throws IOException {
    
    assert blockNo > 0;
    assert maxLookback >= 0;
    var blockNos = targetBlockNos();
    final int lastIndex = Math.max(0, blockNos.size() - maxLookback);
    for (int index = blockNos.size(); index-- > lastIndex; ) {
      long bn = blockNos.get(index);
      if (bn <= 0)
        throw new SerialFormatException(
          "block no. " + bn + " at index [" + index + "]");
      if (bn == blockNo)
        return false;
    }

    long pos = tbnOffset(tbnCount());
    var cell = ByteBuffer.allocate(8).putLong(blockNo).flip();
    ChannelUtils.writeRemaining(tbn, pos, cell);
    return true;
  }



  protected List<Long> targetBlockNos() {
    try {
      return Lists.functorList(tbnCount(), this::getTargetBlockNo);
    
    } catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }





  private long tbnOffset(int index) {
    return index * 8L;
  }


  private long getTargetBlockNo(int index) {
    try {
      var cell = ByteBuffer.allocate(8);
      long pos = tbnOffset(index);
      long blockNo = ChannelUtils.readRemaining(tbn, pos, cell).flip().getLong();
      assert blockNo > 0;
      return blockNo;
    } catch (IOException iox) {
      throw new UncheckedIOException(
        "on index " + index + ": " + iox.getMessage(), iox);
    }
  }


  protected int tbnCount() throws IOException {
    int count = (int) (tbn.size() / 8L);
    assert count >= 0;
    return count;
  }



  /** Invoked <em>after</em> {@link #highestCommon(BlockProof)} */
  protected boolean saveChainState(BlockProof state, long tbn)
      throws HashConflictException, UncheckedIOException {
    
    highestCommon(state);


    File target = proposeStateFile(state);
    File staged = newStagedFile(target.getName());
    FileUtils.writeNewFile(staged, state.serialize());
    return staged.renameTo(target);
  }



  


  protected File tbnFile() {
    return new File(dir, TBN_FILE);
  }



  /**
   * Returns the 
   * @param state
   * @return
   * @throws HashConflictException
   */
  protected long highestCommon(BlockProof state) throws HashConflictException {

    return getHcbn(state).highestCommonBlockNo();
  }


  protected Hcbn getHcbn(BlockProof state) throws HashConflictException {

    long hbn = 0L;
    BlockProof blockProof = null;
    for (var bp : listBlockProofs()) {
      long bn = state.highestCommonBlockNo(bp);
      if (bn > hbn) {
        hbn = bn;
        blockProof = bp;
      } else if (
          bn == hbn &&
          bn != 0 &&
          blockProof.blockNo() < bp.blockNo()) {

        blockProof = bp;
      }
    }
    return new Hcbn(hbn, blockProof);
  }


  protected record Hcbn(long highestCommonBlockNo, BlockProof blockProof) {
    protected Hcbn {
      
      if (blockProof == null) {

        if (highestCommonBlockNo != 0)
          throw new IllegalArgumentException(
            "non-zero highestCommonBlockNo (" + highestCommonBlockNo +
            ") with non-null blockProof");
      
      } else if (highestCommonBlockNo <= 0) {

        var msg = highestCommonBlockNo == 0 ?
            "blockProof must be null with highestCommonBlockNo set to zero" :
            "negative highestCommonBlockNo: " + highestCommonBlockNo;
        throw new IllegalArgumentException(msg);
      } 
    }
  }




  public Crum saveTrail(Crumtrail trail)
      throws HashConflictException, UncheckedIOException {

    highestCommon(trail.blockProof());

    var hash = trail.crum().hash();
    var hex = IntegralStrings.toHex(hash);
    File crumFile = trailTree.find(hex);

    if (crumFile != null)
        return verifyCrumtrail(crumFile, trail);
    
    
    File target = trailTree.suggest(hex);
    File staging = newStagedFile(target.getName());
    
    try (var ch = Opening.CREATE.openChannel(staging)) {
      
      ChannelUtils.writeRemaining(ch, trail.serialize());


      if (!staging.renameTo(target)) {
        if (target.exists())
          return verifyCrumtrail(target, trail);
        
        throw new IoException(
            "failed to move staged file: " + staging + " --> " + target);
      }
    
    } catch (IOException iox) {
      throw new UncheckedIOException("on writing " + staging, iox);
    }
        
    try {
      recordTargetBlockNo(trail.blockNo()); // not checking return value
                                            // cuz if previously failed at next
                                            // step, wanna make progress

      
    } catch (IOException iox) {
      throw new UncheckedIOException(
          "on writing TBN [" + trail.blockNo() + "]: " + iox.getMessage(), iox);
    }


    return trail.crum();
  }

  /** Workaround UncheckedIOException not taking null as cause. */
  private static class IoException extends UncheckedIOException {
    public IoException(String msg) {
      this(msg, new IOException());
    }
    private IoException(String msg, IOException dummy) {
      super(msg, dummy);
    }
  }



  /**
   * Verifies the new trail against an existing crumtrail file.
   * 
   * @param trailFile   existing crumtrail file
   * @param trail       input trail
   * @return            the crum in the existing trail file
   * 
   * @throws SerialFormatException  if the wrong crum hash in {@code trailFile}
   * 
   */
  private Crum verifyCrumtrail(File trailFile, Crumtrail trail)
      throws SerialFormatException {

    var mem = FileUtils.loadFileToMemory(trailFile);
    var trailOfRecord = Crumtrail.load(mem);

    if (!trail.crum().hash().equals(trailOfRecord.crum().hash()))
      throw new SerialFormatException(
        "corrupted (wrong hash): " + trailFile);

    return trailOfRecord.crum();
  }


  private final static int MAX_CRNS = 4;
  private final static int MAX_NUM_DOTTED_STATE_FILES = 16;

  protected File proposeStateFile(BlockProof state) {
    
    var crns = Lists.reverse(state.chainState().pack().compressedRowNos());
    final int maxCrns = Math.min(crns.size(), MAX_CRNS);

    String prefix = Long.toString(crns.get(0));
    File file = new File(dir, prefix + chainExt);
    if (!file.exists())
      return file;

    for (int index = 1; index < maxCrns; ++index) {

      prefix += '-' + crns.get(index);
      file = new File(dir, prefix + chainExt);
      if (!file.exists())
        return file;
    }

    for ( int postPrefix = 1;
          postPrefix < MAX_NUM_DOTTED_STATE_FILES;
          ++postPrefix) {
      
      var name = prefix + '.' + postPrefix + chainExt;
      file = new File(dir, name);
      if (file.exists())
        return file;
    }
    throw new IllegalStateException(
        "too many dot-number state files for " + prefix + chainExt);
  }



  


  public List<BlockProof> listBlockProofs() {
    List<File> stateFiles = listBlockFiles();
    if (stateFiles.isEmpty())
      return List.of();
    
    var blockProofs = new BlockProof[stateFiles.size()];
    for (int index = blockProofs.length; index-- > 0; )
      blockProofs[index] = loadBlockProof(stateFiles.get(index));
    
    return Lists.asReadOnlyList(blockProofs);
  }



  protected List<File> listBlockFiles() {
    File[] stateFiles = dir.listFiles(new FilenameFilter() {
      @Override public boolean accept(File dir, String name) {
        return name.endsWith(chainExt);
      }
    });
    return Lists.asReadOnlyList(stateFiles);
  }


  

  



  private BlockProof loadBlockProof(File stateFile) {
    var mem = FileUtils.loadFileToMemory(stateFile);
    return BlockProof.load(mem);
  }


  private File newStagedFile(String postfixName) {
    return new File(stagingDir, RandomId.RUN_INSTANCE + "_" + postfixName);
  }

}
