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
 * A local repo for {@linkplain CrumTrail}s.
 */
public class TrailRepo implements Closeable {
  
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
   * Creates a new instance.
   */
  public TrailRepo(File dir, Opening opening) throws IOException {
    
    this.dir = Objects.requireNonNull(dir, "null dir");
    Objects.requireNonNull(opening, "null opening");
    
    
    try (TaskStack onFail = new TaskStack(this)) {
      
      this.idx = Table.newSansKeystoneInstance(
          opening.openChannel(new File(dir, IDX_FILE)),
          IDX_ROW_WIDTH);
      
      onFail.pushClose(idx);
      
      this.blobs = opening.openChannel(new File(dir, BLOB_FILE));
      
      onFail.clear();
    }
  }
  
  

  
  
  /**
   * Returns the directory this repo is rooted at.
   */
  public final File getDir() {
    return dir;
  }
  
  
  
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
  
  
  
  public long size() {
    try {
      return idx.getRowCount();
    } catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
  
  
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
    TaskStack closer = new TaskStack(this);
    closer.pushClose(blobs);
    closer.pushClose(idx);
    closer.close();
  }

}
