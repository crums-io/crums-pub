/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;

import java.util.Objects;

import io.crums.sldg.json.HashEncoding;
import io.crums.util.json.JsonEntityParser;

/**
 * Boilerplate base type for parsers. Parsers commonly need
 * to know how 32-byte hashes are to be encoded.
 */
public abstract class BaseParser<T> implements JsonEntityParser<T> {

  /**
   * Codec for 32-byte values.
   */
  protected final HashEncoding hashCodec;

  /**
   * Sets (fixes) the parser's hash codec.
   * 
   * @param hashCodec  not null
   * 
   * @see #hashCodec
   */
  protected BaseParser(HashEncoding hashCodec) {
    this.hashCodec = Objects.requireNonNull(hashCodec, "null hash codec");
  }

}
