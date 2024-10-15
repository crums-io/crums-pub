/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.client;



import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.crums.io.FileUtils;
import io.crums.sldg.HashConflictException;
import io.crums.sldg.Path;
import io.crums.stowkwik.io.HexPathTree;
import io.crums.tc.BlockProof;
import io.crums.tc.Constants;
import io.crums.tc.Crum;
import io.crums.tc.Crumtrail;
import io.crums.util.IntegralStrings;
import io.crums.util.RandomId;


/**
 * Local repo for crumtrails from a <em>single</em> timechain.
 * 
 * <h2>Assumptions</h2>
 * 
 * <ol>
 * <li><em>Sequential Writes.</em></li>
 * <li><em>Uncondensed Crumtrails.</em></li>
 * <li><em>Concurrent Reads.</em> </li>
 * </ol>
 * 
 * <h2>Basic Design</h2>
 * <p>
 * It's maybe easiest to describe in two parts: writing and reading,
 * in that order..
 * </p>
 * <h3>Writing</h3>
 * <p>
 * Crumtrails are keyed by the witnessed hash ({@linkplain Crum#hash()})
 * using a filepath naming scheme called a hex tree (similar to how git
 * does it). When a crumtrail is saved ({@linkplain #add(Crumtrail)}),
 * first the repo's "global" chain [block] proof is updated (up to the
 * block no. of the crumtrail). Barring any hash conflicts in the 1st step,
 * the crumtrail is next saved (in binary format) in a file under the hex tree.
 * </p>
 * <h3>Reading / Lookup</h3>
 * <p>
 * Finding and returnng crumtrail for a witnessed hash is also a 2 step
 * process. First, a crumtrail for that hash is looked up and loaded
 * from the hex tree directory structure. Second, assuming it was found,
 * the loaded crumtrail is updated with the chain's latest block proof
 * before being returned.
 * </p>
 * <h4>Some Details</h4>
 * <p>
 * All files are <em>write-once</em>; furthermore, all file-writes are
 * <em>staged</em>.
 * The precise rule for concurrency is that the repo's global chain block
 * proof (hereafter <em>chain</em>) can only ever be appended. Altho this
 * class is strict about enforcing these rules, it's also a goal to make it
 * this enforcement process agnostic.
 * </p><p>
 * The "current" chain file (the global block proof) is represented by a
 * numbered file encoding its last (highest) recorded timechain block.
 * Since successive chain files include all ancestor data, lower numbered
 * chain files can be removed without loss of information. (There are
 * race condition checks to be made before deleting these.)
 * </p>
 */
public class TrailRepo {


  public final static String CARGO_PROOF_EXT = ".crums";
  public final static String BLOCK_PROOF_EXT = ".chain";

  private final static int CPX_LEN = CARGO_PROOF_EXT.length();

  /** Staging directory name. */
  public final static String STAGING = "staging";


  private final static FilenameFilter CHAIN_FILTER =
      new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
          return name.endsWith(CARGO_PROOF_EXT);
        }
      };


  protected final File dir;
  protected final File stagingDir;
  protected final HexPathTree trailTree;



  public TrailRepo(File dir) {
    this.dir = dir;
    this.stagingDir = FileUtils.ensureDir(new File(dir, STAGING));
    this.trailTree = new HexPathTree(dir, CARGO_PROOF_EXT);
  }


  /**
   * Directory this instance lives in.
   */
  public final File dir() {
    return dir;
  }


  /**
   * Finds and returns a crumtrail without lineage.
   * 
   * @param hash        32-byte hash
   * 
   * @return {@code findTrail(hash, false)}
   * 
   * @see #findTrail(ByteBuffer, boolean)
   */
  public Optional<Crumtrail> findTrail(ByteBuffer hash) {
    return findTrail(hash, false);
  }

  /**
   * Finds and returns a crumtrail for the given 32-byte hash.
   * 
   * @param hash        32-byte hash
   * @param incLineage  if {@code true} the trail's lineage from the genesis block
   *                    is included
   */
  public Optional<Crumtrail> findTrail(ByteBuffer hash, boolean incLineage) {
    if (hash.remaining() != Constants.HASH_WIDTH)
      throw new IllegalArgumentException("illegal hash length: " + hash);
    
    String hex = IntegralStrings.toHex(hash);
    File trailFile = trailTree.find(hex);
    if (trailFile == null)
      return Optional.empty();
    
    Crumtrail trail = Crumtrail.load(FileUtils.loadFileToMemory(trailFile));
    // sanity check
    if (!trail.crum().hash().equals(hash))
      throw new IllegalStateException(
          "crum hash conflicts for hex path " + trailFile);

    final long tbn = trail.blockNo();
    var chainState = chainState();
    var blockProof = chainState.forBlockNo(tbn, incLineage)
        .orElseThrow(() -> new IllegalStateException(
            "chain block proof [" + chainState.blockNo() +
            "] does not contain trail block [" + trail.blockNo() + "] for " +
            trail.crum()));

    return Optional.of(trail.setBlockProof(blockProof));
  }



  /**
   * Adds the given crumtrail to the repo. Crumtrails are assumed to be added
   * in order of creation.
   * 
   * @param trail   the new trail
   * 
   * @throws HashConflictException
   *          if the given trail block hashes conflict with those of the
   *          timechain recorded in this repo
   *          
   */
  public void add(Crumtrail trail) throws HashConflictException {
    if (trail.isCondensed())
      throw new IllegalArgumentException("condensed crumtrail " + trail);

    var repoChain = chainState();
    if (repoChain == null) {
      init(trail);
      return;
    }

    // Update the repo's block proof, first

    var trailChain = trail.blockProof();

    final long trailBn = trail.blockNo();
    final long repoBn = repoChain.blockNo();

    if (trailBn < repoBn) {
      
      if (!repoChain.chainState().hasRow(trailBn))
        throw new IllegalArgumentException(
            "trail block [" + trailBn +
            "] is (behind and) not contained in repo " + this +
            "'s block proof ending at block [" + repoBn + "]");
      
      assertBlockHash(repoChain, trailChain, trailBn);
      
      // otherwise, thump up

    } else if (trailBn > repoBn) {

      // following throws HCE on mismatched hashes
      // or IAE on mismatched chain params..

      final long hcbn = trailChain.highestCommonBlockNo(repoChain);
      if (hcbn == 0)
        throw new IllegalArgumentException(
          "trail chain (block proof) does not intersect with " +
          "chain recorded in repo " + this);

      if (hcbn != repoBn)
        throw new IllegalArgumentException(
          "block proof in trail does not reference block [" +
          repoBn + "], the last block in repo " + this);

      

      Path newStatePath =
          repoChain.chainState().appendTail(
              trailChain.chainState().headPath(trail.blockNo() + 1));

      var newChain = new BlockProof(repoChain.chainParams(), newStatePath);
      writeChain(newChain);

    } else {

      // assert trailBn == repoBn;
      assertBlockHash(repoChain, trailChain, trailBn);
    }

    // any necessary update to the chain's block proof is completed

    writeTrail(trail);
  }



  private void assertBlockHash(
      BlockProof repoChain, BlockProof trailChain, long blockNo) {
    
    if (!repoChain.chainState().getRowHash(blockNo).equals(
        trailChain.chainState().getRowHash(blockNo)))

      throw new HashConflictException("at block [" + blockNo + "]");
  }


  private void init(Crumtrail trail) {
    final long tbn = trail.blockNo();
    BlockProof chain;
    if (tbn != trail.blockProof().blockNo()) {
      var statePath = trail.blockProof().chainState().headPath(tbn + 1);
      assert statePath.hiRowNumber() == tbn;
      chain = new BlockProof(trail.chainParams(), statePath);
    } else {
      chain = trail.blockProof();
    }
    writeChain(chain);
    writeTrail(trail);
  }



  private boolean writeChain(BlockProof chain) {
    File chainFile = chainFile(chain.blockNo());
    File staged = newStagedFile(chainFile.getName());
    FileUtils.writeNewFile(staged, chain.serialize());
    return commitStaged(staged, chainFile);
  }



  private boolean writeTrail(Crumtrail trail) {
    var hex = trail.crum().hashHex();
    File trailFile = trailTree.suggest(hex);
    File staged = newStagedFile(trailFile.getName());
    FileUtils.writeNewFile(staged, trail.serialize());
    return commitStaged(staged, trailFile);
  }
  

 
  protected final File chainFile(long tbn) {
    return new File(dir, tbn + BLOCK_PROOF_EXT);
  }



  protected List<Long> listChainFilesNos() {
    return chainNos().toList();
  }


  private Stream<Long> chainNos() {
    return
        Arrays.asList(dir.list(CHAIN_FILTER)).stream()
        .map(s -> s.substring(0, s.length() - CPX_LEN))
        .map(Long::parseLong);
  }


  public BlockProof chainState() {
    return chainNos().max(Long::compare).map(this::loadChain).orElse(null);
  }


  public long blockNo() {
    long bn = chainNos().max(Long::compare).orElse(0L);
    assert bn == 0 || chainState().blockNo() == bn;
    return bn;
  }



  private BlockProof loadChain(long tbn) {
    var mem = FileUtils.loadFileToMemory(chainFile(tbn));
    return BlockProof.load(mem);
  }


  private File newStagedFile(String postfixName) {
    return new File(stagingDir, RandomId.RUN_INSTANCE + "_" + postfixName);
  }


  private boolean commitStaged(File staged, File target) {
    if (staged.renameTo(target))
      return true;
    
    if (!target.exists())
      throw new UncheckedIOException(
          "failed to commit (rename/move) to " + target,
          new IOException());

    return false;
  }







  @Override
  public String toString() {
    return "<" + dir.getName() + ">";
  }


}




