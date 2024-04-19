/*
 * Copyright 2024 Babak Farhang
 */
/**
 * Time chain 2 root package.
 * <h2>TODO</h2>
 * <p>Listed below, in no particular order:</p>
 * <ul>
 * <li>
 * static "Serial" load() methods should throw specific
 * SerialFormatException, instead of IAEs. 
 * </li><li>
 * </li>
 * </ul>
 * <h2>FIXME</h2>
 * <p>Gathered below, in no particular order:</p>
 * <ul>
 * <li>
 * Fix SerialFormatException.newMessage(..) when cause.getMessage()
 * is null (BufferUnderflowException's message, for eg, might).
 * </li><li>
 * </li>
 * </ul>
 */
package io.crums.tc;