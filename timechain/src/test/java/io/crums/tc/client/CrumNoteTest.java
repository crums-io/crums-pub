/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.client;


import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.crums.tc.Constants;
import io.crums.tc.Crum;
import io.crums.tc.client.Repo.CrumNote;

public class CrumNoteTest {


  @Test
  public void testRoundtrip() {
    
    var mockHash = mockHash();
    var host = "example.com";
    long utc = System.currentTimeMillis();

    var note = new Repo.CrumNote(new Crum(mockHash, utc), host);
    var name = note.toName();
    var rt = CrumNote.fromName(name);
    assertEquals(note, rt);
  }

  private ByteBuffer mockHash() {
    return mockHash((byte) 0);
  }

  private ByteBuffer mockHash(byte start) {
    var mockHash = ByteBuffer.allocate(Constants.HASH_WIDTH);
    for (byte i = 0; i < Constants.HASH_WIDTH; ++i)
      mockHash.put(i);
    assert !mockHash.hasRemaining();
    return mockHash.flip();
  }

}

