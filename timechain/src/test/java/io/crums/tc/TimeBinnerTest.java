/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;


import static io.crums.tc.TimeBinner.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.crums.testing.SelfAwareTestCase;

/**
 * 
 */
public class TimeBinnerTest extends SelfAwareTestCase {

  @Test
  public void testQuarterSec() {
    long now = System.currentTimeMillis();
    long binTime = QUARTER_SEC.binTime(now);
    long binNo = QUARTER_SEC.toBinNo(now);
    assertTrue(binTime <= now);
    assertTrue(binTime >= now - 256);
    assertEquals(256, QUARTER_SEC.duration());
    

    System.out.println();
    System.out.println(method(new Object() { }) + ":");
    System.out.println(QUARTER_SEC);
    System.out.println("resolution: " + QUARTER_SEC.duration() + " millis");
    System.out.println("mask:       " + Long.toBinaryString(QUARTER_SEC.mask));
    System.out.println("now:        " + Long.toBinaryString(now));
    System.out.println("bin time:   " + Long.toBinaryString(binTime));
    System.out.println("bin no.:    " + Long.toBinaryString(binNo));
  }
  
  @Test
  public void testBinSpinPerf() {
    System.out.println();
    System.out.println(method(new Object() { }) + ":");
    int count = 1;
    for (long now = EIGTH_SEC.binTime(System.currentTimeMillis());
        now == EIGTH_SEC.binTime(System.currentTimeMillis());
        ++count);
    System.out.println(EIGTH_SEC + " count: " + count);
  }
  
  @Test
  public void testForExponent() {
    for (int e = TimeBinner.MIN_EXP; e <= TimeBinner.MAX_EXP; ++e) {
      assertEquals(e, TimeBinner.forExponent(e).binExponent());
    }
  }
  
  @Test
  public void testDay() {
    long duration = HOUR_19.duration();
    long realDay = 24 * 3600 * 1000;
    long realDayDeficit = realDay - duration;
    
    System.out.println();
    System.out.println(method(new Object() { }) + ":");
    System.out.println(HOUR_19);
    System.out.println("duration: " + duration);
    System.out.println("real day: " + realDay);
    System.out.println("deficit:  " + realDayDeficit);
    System.out.println("    or    " + (realDayDeficit / 3_600_000.0) + " hr");
  }

}
