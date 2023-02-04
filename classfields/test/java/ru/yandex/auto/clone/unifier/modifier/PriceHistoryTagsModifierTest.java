package ru.yandex.auto.clone.unifier.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.auto.core.dealer.DealerOrgType;
import ru.yandex.auto.core.model.PriceHistoryEntry;
import ru.yandex.auto.core.model.UnifiedCarInfo;
import ru.yandex.auto.core.stock.http.Currency;

/**
 * Created by sanekas on 26/04/2017. Тесты на условия: 1) Показываем history_discount только если:
 * разница между датой последней (новой) цены и датой предыдущей цены >= 3 дня & (ограничение сверху
 * <= 7 дней) 2) Показываем history_discount только если: изменение не больше 50% от цены
 */
public class PriceHistoryTagsModifierTest {
  private static final String DEFAULT_CURRENCY = Currency.RUR.getAliases()[0];
  private static final int DEFAULT_PRICE = 300_000;
  private static final String HISTORY_DISCOUNT_TAG = "history_discount";

  private PriceHistoryTagsModifier tagsModifier;

  @Before
  public void setUp() {
    tagsModifier = new PriceHistoryTagsModifier();
  }

  /**
   * Тест на условие: разница между датой последней (новой) цены и датой предыдущей цены >= 2 дня
   * Diff = 1 дням (6 - 5) => Тэга быть не должно
   */
  @Test
  public void testTimeDiffBetweenLastPriceChangeTwoDays() {
    final UnifiedCarInfo carInfo = configureTest(50_000, 5, 6);
    Assert.assertFalse(carInfo.getSearchTags().contains(HISTORY_DISCOUNT_TAG));
  }

  /** Diff = 4 дням (6 - 2) => Тэг должен быть */
  @Test
  public void testTimeDiffBetweenLastPriceChangeFourDays() {
    final UnifiedCarInfo carInfo = configureTest(50_000, 2, 6);
    Assert.assertTrue(carInfo.getSearchTags().contains(HISTORY_DISCOUNT_TAG));
  }

  /** Показываем history_discount только если: изменение не больше 50% от цены Diff > 50% */
  @Test
  public void testPriceDiffBetweenLastChangesMoreFifty() {
    final UnifiedCarInfo carInfo = configureTest(160_000, 2, 6);
    Assert.assertFalse(carInfo.getSearchTags().contains(HISTORY_DISCOUNT_TAG));
  }

  /** Показываем history_discount только если: изменение не больше 50% от цены Diff < 50% */
  @Test
  public void testPriceDiffBetweenLastChangesLessFifty() {
    final UnifiedCarInfo carInfo = configureTest(80_000, 2, 6);
    Assert.assertTrue(carInfo.getSearchTags().contains(HISTORY_DISCOUNT_TAG));
  }

  /** Показываем изменения цен только для объявлений, которые старее 2 дней */
  @Test
  public void testCreationDateOlderThanThreeDays() {
    final UnifiedCarInfo carInfo = configureTest(80_000, 1, 2);
    Assert.assertFalse(carInfo.getSearchTags().contains(HISTORY_DISCOUNT_TAG));
  }

  /** Показываем только изменение цены, произошедшее за последние 7 суток */
  @Test
  public void testPriceChangeForLastSevenDays() {
    final UnifiedCarInfo carInfo = configureTest(80_000, 9, 10);
    Assert.assertFalse(carInfo.getSearchTags().contains(HISTORY_DISCOUNT_TAG));
  }

  @NotNull
  private UnifiedCarInfo configureTest(
      final int priceChange, final int sndDayChange, final int creationDaysBack) {
    final UnifiedCarInfo carInfo = new UnifiedCarInfo("1");
    final List<PriceHistoryEntry> priceHistory = new ArrayList<>();
    final long currentTime = System.currentTimeMillis();

    carInfo.setDealerOrgType(DealerOrgType.OFFICIAL_DEALER.getType());
    carInfo.setCreationDate(currentTime - TimeUnit.DAYS.toMillis(creationDaysBack));

    priceHistory.add(
        new PriceHistoryEntry(
            DEFAULT_PRICE - priceChange,
            currentTime - TimeUnit.DAYS.toSeconds(sndDayChange),
            DEFAULT_CURRENCY));
    priceHistory.add(
        new PriceHistoryEntry(
            DEFAULT_PRICE,
            currentTime - TimeUnit.DAYS.toSeconds(creationDaysBack),
            DEFAULT_CURRENCY));

    carInfo.setPriceHistoryEntries(priceHistory);
    tagsModifier.modify(carInfo);
    return carInfo;
  }
}
