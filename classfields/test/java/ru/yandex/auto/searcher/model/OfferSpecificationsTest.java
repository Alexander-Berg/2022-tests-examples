package ru.yandex.auto.searcher.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

public class OfferSpecificationsTest {

  @Test
  public void testEngineVolumeRounding() {
    Assert.assertEquals(new Double(1d), getOfferWithEngineVolume(998).getEngineCapacity());
    Assert.assertEquals(new Double(1.2d), getOfferWithEngineVolume(1154).getEngineCapacity());
    Assert.assertEquals(new Double(1.2d), getOfferWithEngineVolume(1149).getEngineCapacity());
    Assert.assertEquals(new Double(1.2d), getOfferWithEngineVolume(1145).getEngineCapacity());
    Assert.assertEquals(new Double(1.1d), getOfferWithEngineVolume(1144).getEngineCapacity());
    Assert.assertEquals(new Double(1.1d), getOfferWithEngineVolume(1140).getEngineCapacity());
    Assert.assertEquals(new Double(1.1d), getOfferWithEngineVolume(1139).getEngineCapacity());
    Assert.assertEquals(new Double(2.9d), getOfferWithEngineVolume(2849).getEngineCapacity());
    Assert.assertEquals(new Double(5.1d), getOfferWithEngineVolume(5139).getEngineCapacity());
    Assert.assertEquals(new Double(51.4d), getOfferWithEngineVolume(51399).getEngineCapacity());
  }

  @NotNull
  @Contract("_ -> new")
  private OfferSpecifications getOfferWithEngineVolume(Integer volume) {
    return new OfferSpecifications(
        null, null, null, null, null, null, null, null, null, null, volume, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
  }
}
