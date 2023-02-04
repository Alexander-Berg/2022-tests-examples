package ru.yandex.auto.core.vin;

import static org.junit.Assert.*;

import org.junit.Test;

/** @author alesavin */
public class VinValidatorTest {

  @Test
  public void testValidate() {
    assertFalse(VinValidator.isValid("111111111111111111"));
    assertFalse(VinValidator.isValid("1111111111111111_"));
    assertFalse(VinValidator.isValid(null));
    assertFalse(VinValidator.isValid(""));
    assertFalse(VinValidator.isValid("aaa"));
  }

  @Test
  public void testValidateByExamples() {
    assertTrue(VinValidator.isValid("11111111111111111"));
    assertTrue(VinValidator.isValid("00000000000000000"));
    assertTrue(VinValidator.isValid("12345678901234567"));
    assertTrue(VinValidator.isValid("12345678912345678"));
    assertTrue(VinValidator.isValid("77777777777777777"));
    assertTrue(VinValidator.isValid("Xxxxxxxxxxxxxxxxx"));
    assertTrue(VinValidator.isValid("1234567890ABCDEFG"));
    assertTrue(VinValidator.isValid("12345678910111213"));
    assertTrue(VinValidator.isValid("wwwwwwwwwwwwwwwww"));
    assertTrue(VinValidator.isValid("88888888888888888"));
    assertTrue(VinValidator.isValid("12345678909876543"));
    assertTrue(VinValidator.isValid("XUFCD26FJ8A005254"));
    assertTrue(VinValidator.isValid("55555555555555555"));
    assertTrue(VinValidator.isValid("SALLMAM549A298499"));

    assertFalse(VinValidator.isValid("отсутствует"));

    assertTrue(VinValidator.isValid("wdb2093651f008811"));
    assertTrue(VinValidator.isValid("KNMCSHLMSCP879670"));
    assertTrue(VinValidator.isValid("WBAGN61080DP95564"));
    assertTrue(VinValidator.isValid("JMBSNCY2A8U002099"));
    assertTrue(VinValidator.isValid("JMZCR19F780316295"));
    assertTrue(VinValidator.isValid("VF1KZ090347141867"));
    assertTrue(VinValidator.isValid("X7LLSRB2HAH326525"));
    assertTrue(VinValidator.isValid("33333333333333333"));
    assertTrue(VinValidator.isValid("JN1TANZ51U0003602"));
    assertTrue(VinValidator.isValid("JN1WNYD21U0000001"));
    assertTrue(VinValidator.isValid("4T3ZA3BB6AU027852"));
    assertTrue(VinValidator.isValid("WVWZZZ1KZ9W812335"));
    assertTrue(VinValidator.isValid("WBSLX910X0C986396"));
    assertTrue(VinValidator.isValid("Z94CU41CBCR128265"));
    assertTrue(VinValidator.isValid("YV1MK204282048255"));
  }
}
