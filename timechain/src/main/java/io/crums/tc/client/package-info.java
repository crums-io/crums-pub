/*
 * Copyright 2024 Babak Farhang
 */
/**
 * Client-side utilities. Remote access to timechain servers; local
 * storage structures for saving crumtrails.
 * 
 * <h2>Implementation</h2>
 * 
 * The no frills class for accessing a <em>single</em> remote timechain is
 * {@linkplain RemoteChain}. Usually, you also want to record the interaction
 * with the timechain: {@linkplain TrailRepo} provides file-based storage for
 * crumtrails from a <em>single</em> chain. {@linkplain Repo}, in turn,
 * encapsulates a <em>collection</em> of {@code TrailRepo}s. Finally,
 * {@linkplain Client}, is the {@code Repo}-aware, multi-chain accessor
 * targeted for end-use.
 * 
 */
package io.crums.tc.client;