/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


/**
 * Exception superclass.
 */
@SuppressWarnings("serial")
public abstract class CrumsException extends RuntimeException {

  public CrumsException(String message) {
    super(message);
  }

  public CrumsException(Throwable cause) {
    super(cause);
  }

  public CrumsException(String message, Throwable cause) {
    super(message, cause);
  }

  public CrumsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


  /**
   * So you can do factory-style exception-handling. E.g. you can
   * have a factory method {@code buildException()}
   * which you can invoke this way with a clearer stack trace.
   * <p><pre>
   * 
   *  try {
   *    // .. do some work
   *    ..
   *  } catch (CrumsException cx) {
   *    throw buildException(cx).fillInStackTrace();
   *  }
   * 
   * </pre></p>
   */
  @Override
  public synchronized CrumsException fillInStackTrace() {
    super.fillInStackTrace();
    return this;
  }
  
  
  /**
   * Returns the root cause. This is just the last, non-null {@linkplain #getCause() cause}.
   * 
   * @return the last non-null cause, if any; {@code null}, o.w.
   */
  public Throwable getRootCause() {
    Throwable cause = getCause();
    if (cause == null)
      return null;
    for (; cause.getCause() != null; cause = cause.getCause());
    return cause;
  }

}
