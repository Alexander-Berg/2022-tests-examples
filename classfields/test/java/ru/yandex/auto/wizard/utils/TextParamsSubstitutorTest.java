package ru.yandex.auto.wizard.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class TextParamsSubstitutorTest {

  private String getHolderFor(String name) {
    return String.format("${%s}", name.toUpperCase());
  }

  @Test
  public void countSubstitute_Ok() {
    String holder = getHolderFor("ad_count");
    String template = String.format("Количество объявлений: %s", holder);

    TextParamsSubstitutor substitutor = TextParamsSubstitutor.newSubstitutor(template);
    substitutor.setAdCount(1000);

    String result = template.replace(holder, "1000");
    Assert.assertEquals(result, substitutor.substitute());
  }

  @Test
  public void checkMinimumPrice() {
    String holder = getHolderFor("price_minimum");
    String template = String.format("Minimum price is: %s", holder);

    TextParamsSubstitutor substitutor = TextParamsSubstitutor.newSubstitutor(template);
    substitutor.setMinimumPrice(400000, false);

    String result = template.replace(holder, StringUtils.EMPTY);
    Assert.assertEquals(result.trim(), substitutor.substitute());
  }

  @Test
  public void checkMinimumPriceWithSpaces() {
    String holder = getHolderFor("price_minimum");
    String template = String.format("Minimum price is: %s", holder);
    TextParamsSubstitutor substitutor = TextParamsSubstitutor.newSubstitutor(template);
    substitutor.setMinimumPrice(1000000, true);

    String result = template.replace(holder, "от 1 000 000 ₽");
    Assert.assertEquals(result, substitutor.substitute());
  }

  @Test
  public void transmissionSubstitute_Ok() {
    String holder = getHolderFor("transmission");
    String template = String.format("C коробкой %s", holder);

    TextParamsSubstitutor substitutor = TextParamsSubstitutor.newSubstitutor(template);
    substitutor.setTransmission("механика");

    String result = template.replace(holder, "механика");
    Assert.assertEquals(result, substitutor.substitute());
  }
}
