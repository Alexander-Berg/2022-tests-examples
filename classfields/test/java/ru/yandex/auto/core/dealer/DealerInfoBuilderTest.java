package ru.yandex.auto.core.dealer;

import junit.framework.TestCase;

/** @author Alexander Astakhov leftie@yandex-team.ru Date: Nov 17, 2008 */
public class DealerInfoBuilderTest extends TestCase {

  public void testParsePhoneWithCodeAndNumber() {
    final String phone = "(495)234-33-66";
    assertEquals("+7 (495) 234-33-66", DealerInfo.normalizePhone(phone));
  }

  public void testParsePhoneWithCodeSpacesAndNumber() {
    final String phone = "(48439) 5-70-07";
    assertEquals("+7 (48439) 5-70-07", DealerInfo.normalizePhone(phone));
  }

  public void testParsePhoneWithCodeWithoutBracesSpaceAndNumber() {
    final String phone = "4912 30-75-56";
    assertEquals("+7 (4912) 30-75-56", DealerInfo.normalizePhone(phone));
  }

  public void testParsePhoneWithPrefixCodeWithBracesNoSpaceAndNumber() {
    final String phone = "+7 (812)4356573";
    assertEquals("+7 (812) 4356573", DealerInfo.normalizePhone(phone));
  }

  public void testParsePhoneWithAltPrefixCodeWithBracesNoSpaceAndNumber() {
    final String phone = "8 (812)4356573";
    assertEquals("8 (812) 4356573", DealerInfo.normalizePhone(phone));
  }

  public void testParsePhoneWithAllMinusesAsSeparator() {
    final String phone = "8-499-618-78-56";
    assertEquals("8-499-618-78-56", DealerInfo.normalizePhone(phone));
  }
}
