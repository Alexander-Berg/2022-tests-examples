package ru.auto.tests.desktop.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import ru.auto.tests.desktop.mock.beans.userOfferStats.Counter;
import ru.auto.tests.desktop.mock.beans.userOfferStats.StatsItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.stream;
import static ru.auto.tests.desktop.mock.beans.userOfferStats.PriceInfo.priceInfo;
import static ru.auto.tests.desktop.mock.beans.userOfferStats.StatsItem.stats;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;

public class MockUserOfferStats {

    private static final String ITEMS = "items";


    @Getter
    @Setter
    private JsonObject body;

    private MockUserOfferStats() {
        JsonArray items = new JsonArray();
        body = new JsonObject();
        body.add(ITEMS, items);
    }

    public static MockUserOfferStats userOfferStats() {
        return new MockUserOfferStats();
    }

    public static MockUserOfferStats userOfferStatsForLastDays(String offerId) {
        return userOfferStats().setStats(
                stats().setOfferId(offerId).setCounters(getCountersForLastDays(5)));
    }

    public static Counter getCounter() {
        return Counter.counter()
                .setViews(getRandomBetween(200, 1000))
                .setPhoneCalls(getRandomBetween(10, 100))
                .setPhoneViews(getRandomBetween(100, 500))
                .setFavorite(getRandomBetween(10, 100))
                .setFavoriteRemove(getRandomBetween(1, 10))
                .setFavoriteTotal(getRandomBetween(100, 500))
                .setPriceInfo(
                        priceInfo().setCreateTimestamp("1559219106000")
                                .setCurrency("RUR")
                                .setPrice(700000)
                                .setDprice(700000));
    }

    public MockUserOfferStats setStats(StatsItem... stats) {
        stream(stats).forEach(statsItem -> body.getAsJsonArray(ITEMS).add(getJsonObject(statsItem)));
        return this;
    }

    public static List<Counter> getCountersForLastDays(int daysCount) {
        List<Counter> counters = new ArrayList<>();

        for (int i = daysCount - 1; i >= 0; i--) {
            counters.add(getCounter().setDate(getDateDaysAgo(i)));
        }

        return counters;
    }

    public static String getDateDaysAgo(int daysCount) {
        return LocalDate.parse(LocalDate.now().toString()).minusDays(daysCount)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

}
