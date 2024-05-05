/*
 * Copyright 2024 Babak Farhang
 */
/**
 * Defines the notary data structures and procedures for accepting new
 * [SHA-256] hashes for witness and vending out new
 * {@linkplain io.crums.tc.Crumtrail crumtrail}s.
 * 
 * <h2>Concurrency Model</h2>
 * <p>
 * The notary is designed to support multiple concurrent readers and
 * writers, from multiple processes (not just threads). 
 * </p>
 * <h3>Requirements / Assumptions</h3>
 * <p>
 * The implementation leans heavily on 2 components not directly in
 * its control.
 * </p>
 * <h4>File System</h4>
 * <p>
 * We use the file system mostly as a simple key value store. We could
 * introduce a more generalized abstraction for this layer (so that you
 * could use something like S3 instead), but for now, we haven't.
 * </p>
 * <ol>
 * <li><strong>File moves/renaming:</strong> If <em>n</em> concurrent
 * threads of execution attempt to move <em>n</em> distinct files to
 * the same location, then one and only one will succeed.</li>
 * <li><strong>Write commitment:</strong> If a file is written, closed and
 * then moved to a new location, it's contents are already committed
 * to the file.</li>
 * </ol>
 * <p>
 * Both these requirements are typically "guaranteed" by file systems
 * (whether local, or network mounted).
 * </p>
 * <h4>System Time / Clock Skew</h4>
 * <p>
 * We assume when there are multiple processes or machines accessing the
 * file system, they're in reasonable agreement about wall time. The finer
 * the resolution of the time chain blocks, the greater this precision must
 * be.
 * </p>
 * <h3>Tactics</h3>
 * <p> TODO: explain
 * </p>
 * <h4>Write Once</h4>
 * <p>
 * </p>
 * <h4>Write the Same</h4>
 * <p>
 * </p>
 * 
 * @see io.crums.tc.notary.Notary
 */
package io.crums.tc.notary;