package ru.yandex.auto.core.model.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class SeatsTest {

  @Test
  public void test() {
    assertEquals(5, Seats.extractSeat("seats-5").intValue());
    assertEquals(9, Seats.extractSeat("seats-9").intValue());
    assertEquals(10, Seats.extractSeat("seats-10").intValue());
    assertEquals(10, Seats.extractSeat("10").intValue());
    assertNull(Seats.extractSeat("defefefev"));
  }
}
