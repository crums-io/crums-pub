/*
 * Copyright 2024 Babak Farhang
 */
/**
 * <code>com.sun.net.httpserver</code>-based implementation package.
 * We use virtual threads as a quick way to non-blocking I/O
 * (try this first before using Netty).
 * <em>Package not exported in module-info.</em>
 * <h2>Side Note</h2>
 * <p>
 * A few of the helper classes defined here are
 * reusable and might deserve an independent home.
 * </p>
 */
package io.crums.tc.notary.server;