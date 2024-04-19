/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.client.repo;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.crums.io.IoStateException;
import io.crums.io.Opening;
import io.crums.io.channels.ChannelUtils;
import io.crums.io.store.table.Table;
import io.crums.model.CrumTrail;
import io.crums.util.CachingList;
import io.crums.util.Lists;
import io.crums.util.TaskStack;

/**
 * A local repo for {@linkplain CrumTrail}s. This is a very, very simple design.
 * The repo cares neither about the order crumtrails, nor does it attempt
 * to verify the crumtrails are legit. These concerns are delegated to the user.
 * 
 * <h2>Meta ID</h2>
 * <p>
 * You can specify a <em>meta</em> ID (expressed as a {@code long}) along with
 * each crumtrail you store. The repo does not know what it means: it does not
 * validate it any way. However, the application <em>can</em> order these IDs
 * and then search over them efficiently (even if they don't fit in memory).
 * </p>
 * 
 * @see #putTrail(CrumTrail, long)
 * @see #getIds()
 * @see #getTrail(long)
 */
public class TrailRepo implements Closeable {
  
  /** Tests whether there is a saved repo in the given directory. No side effects. */
  public static boolean isPresent(File dir) {
    return
        new File(dir, IDX_FILE).isFile() &&
        new File(dir, BLOB_FILE).isFile();
  }
  
  /**
   * Path to fixed width index table file.
   */
  public static File indexFile(File dir) {
    return new File(dir, IDX_FILE);
  }
  
  /**
   * Path to sequential blob file containing the {@linkplain CrumTrail}s.
   */
  public static File blobFile(File dir) {
    return new File(dir, BLOB_FILE);
  }
  
  /**
   * Fixed width index table filename.
   * (Created under {@linkplain #getDir() dir}.)
   */
  public final static String IDX_FILE = "ct_idx";
  /**
   * Sequential blob file containing the {@linkplain CrumTrail}s.
   * (Created under {@linkplain #getDir() dir}.)
   */
  public final static String BLOB_FILE = "ct_blobs";
  
  
  private final static int FENCE_WIDTH = 4;
  
  private final static int TRAIL_OVERHEAD = FENCE_WIDTH + Long.BYTES;
  
  private final static ByteBuffer FENCE = ByteBuffer.allocate(FENCE_WIDTH).asReadOnlyBuffer();
  
  
  private final static int IDX_ROW_WIDTH = 16;
  
  
  public static ByteBuffer fence() {
    return FENCE.asReadOnlyBuffer();
  }
  
  
  
  private final File dir;
  
  private final Table idx;
  private final FileChannel blobs;
  
  
  /**
   * Creates a read-write instance. If a repo exists under the given dir, it
   * loads it; otherwise a new repo is created.
   * 
   * @param dir the directory in which the files will be managed
   */
  public TrailRepo(File dir) throws IOException {
    this(dir, Opening.CREATE_ON_DEMAND);
  }

  /**
   * Creates a new instance. Note to access this constructor, you'll need to
   * separately require the <code>io.crums.util</code> module.
   * 
   * @param dir     path to directory
   * @param opening what to do, expected state; requires <code>io.crums.util</code> module
   * 
   * @throws IOException if an error occurs while opening either of the index- or blob files
   */
  public TrailRepo(File dir, @SuppressWarnings("exports") Opening opening) throws IOException {
    
    this.dir = Objects.requireNonNull(dir, "null dir");
    Objects.requireNonNull(opening, "null opening");
    
    
    try (TaskStack onFail = new TaskStack()) {
      
      this.idx = Table.newSansKeystoneInstance(
          opening.openChannel(indexFile(dir)),
          IDX_ROW_WIDTH);
      
      onFail.pushClose(idx);
      
      this.blobs = opening.openChannel(blobFile(dir));
      
      onFail.clear();
    }
  }
  
  

  
  
  /**
   * Returns the directory this repo is rooted at.
   */
  public final File getDir() {
    return dir;
  }
  
  
  /**
   * Puts the given crumtrail into the repo and returns its index.
   * 
   * @param trail the crumtrail
   * @param id     user-specified ID
   * 
   * @return  the trail's index in the repo
   */
  public long putTrail(CrumTrail trail, long id) {
    ByteBuffer blobRecord = ByteBuffer.allocate(trail.serialSize() + TRAIL_OVERHEAD);
    trail.writeTo(blobRecord).putLong(id).clear();
    
    try {
      
      long blobOffset = nextBlobOffset();
      long terminalOffset = blobOffset + blobRecord.remaining();
      
      ChannelUtils.writeRemaining(blobs, blobOffset, blobRecord);

      ByteBuffer idxRow = blobRecord.clear().putLong(terminalOffset).putLong(id).flip();
      
      return idx.append(idxRow);
      
    } catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
  
  
  /**
   * Returns the crumtrail at the given {@code index}.
   * 
   * @param index &ge; 0 and &lt; {@linkplain #size() size}
   * 
   * @see #putTrail(CrumTrail, long)
   */
  public CrumTrail getTrail(long index) {
    if (index < 0)
      throw new IllegalArgumentException("negative index " + index);
    
    try {
      
      if (idx.getRowCount() <= index)
        throw new IllegalArgumentException(
            "index " + index + " out of bounds; size " + idx.getRowCount());
      
      
      final long blobOffset, terminalOffset;
      
      if (index == 0) {
        blobOffset = 0;
        terminalOffset = getIdxRow(index).getLong() - TRAIL_OVERHEAD;
      } else {
        ByteBuffer idxRowsX2 = getIdxRows(index - 1, 2);
        blobOffset = idxRowsX2.getLong(0);
        terminalOffset = idxRowsX2.getLong(IDX_ROW_WIDTH) - TRAIL_OVERHEAD;
      }
      
      
      ByteBuffer serialForm;
      {
        int size = (int) (terminalOffset - blobOffset);
        
        if (blobOffset < 0 || size < 0 || size > MAX_SANITY_READ_BYTES)
          throw new IoStateException(
              "blob offset " + blobOffset + ", terminal offset " + terminalOffset +
              " (size " + size + "), index " + index);
          
        serialForm = ByteBuffer.allocate(size);
      }
      
      ChannelUtils.readRemaining(blobs, blobOffset, serialForm).flip();
      
      return CrumTrail.load(serialForm);
      
    } catch (IOException iox) {
      throw new UncheckedIOException("on getTrail(" + index + ")", iox);
    }
  }
  
  private final static int MAX_SANITY_READ_BYTES = 256 * 1024;
  
  
  /**
   * Returns the number of crumtrails in this repo.
   */
  public long size() {
    try {
      return idx.getRowCount();
    } catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
  
  
  /**
   * Trims the size of the repo. This is a destructive operation.
   * 
   * @param newSize &ge; 0 and &le; {@linkplain #size()}
   */
  public void trimSize(long newSize) {
    try {
      long size = idx.getRowCount();
      if (newSize == size)
        return;
      if (newSize > size)
        throw new IllegalArgumentException("newSize " + newSize + " > size() " + size);
      if (newSize < 0)
        throw new IllegalArgumentException("newSize "  + newSize);


      long terminalOffset =  blobOffset(newSize);
      
      idx.truncate(newSize);
      
      // logically, we're done already:
      // if the next line were commented out, the blobs
      // would simply be overwritten. But we wanna be nice..
      blobs.truncate(terminalOffset);
      
    } catch (IOException iox) {
      throw new UncheckedIOException("on trimSize(" + newSize + ")", iox);
    }
  }
  
  
  /**
   * Returns a lazily loaded list of IDs put along with each crumtrail.
   * 
   * @return ordered list of IDs put into the store (of size {@linkplain #size()})
   * 
   * @see #putTrail(CrumTrail, long)
   */
  public List<Long> getIds() {
    try {
      
      if (idx.isEmpty())
        return Collections.emptyList();
      
      List<ByteBuffer> idxAsList = idx.getListSnapshot();
      return CachingList.cache(Lists.map(idxAsList, r ->  r.getLong(8)));
    
    } catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
  
  /** @return {@code size() == 0} */
  public boolean isEmpty() {
    return size() == 0;
  }
  
  
  private long nextBlobOffset() throws IOException {
    return blobOffset(idx.getRowCount());
  }
  
  private long blobOffset(long row) throws IOException {
    if (row == 0)
      return 0;
    ByteBuffer idxRow = getIdxRow(row - 1);
    return idxRow.getLong();
  }
  
  
  private ByteBuffer getIdxRow(long row) throws IOException {
    return getIdxRows(row, 1);
  }
  
  
  private ByteBuffer getIdxRows(long row, int count) throws IOException {
    ByteBuffer rowData = ByteBuffer.allocate(IDX_ROW_WIDTH * count);
    idx.read(row, rowData);
    return rowData.flip();
  }



  @Override
  public void close() {
    TaskStack closer = new TaskStack();
    closer.pushClose(blobs);
    closer.pushClose(idx);
    closer.close();
  }

}
