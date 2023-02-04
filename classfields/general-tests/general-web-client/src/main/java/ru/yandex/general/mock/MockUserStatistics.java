package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.utils.Utils.formatDate;
import static ru.yandex.general.utils.Utils.getCurrentDate;
import static ru.yandex.general.utils.Utils.getDateEarlier;

public class MockUserStatistics {

    private static final String USER_STATISTICS_TEMPLATE = "mock/userStatistics/userStatisticsTemplate.json";

    private static final String TOTAL_COUNTERS = "totalCounters";
    private static final String ACTIONS_STATISTICS = "actionsStatistics";
    private static final String PREVIOUS_PERIOD_INFO = "previousPeriodInfo";
    private int startDateOffset = 14;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject userStatistics;

    private MockUserStatistics(String pathToTemplate) {
        this.userStatistics = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserStatistics userStatistics() {
        return new MockUserStatistics(USER_STATISTICS_TEMPLATE);
    }

    @Step("Добавляем каунтер «Заработали» = «{counter}»")
    public MockUserStatistics setTotalEarned(int counter) {
        userStatistics.getAsJsonObject(TOTAL_COUNTERS).addProperty("earned", String.valueOf(counter));
        return this;
    }

    @Step("Добавляем каунтер «Активные» = «{counter}»")
    public MockUserStatistics setTotalActive(int counter) {
        userStatistics.getAsJsonObject(TOTAL_COUNTERS).addProperty("activeOffers", counter);
        return this;
    }

    @Step("Добавляем каунтер «Отклоненные» = «{counter}»")
    public MockUserStatistics setTotalBanned(int counter) {
        userStatistics.getAsJsonObject(TOTAL_COUNTERS).addProperty("bannedOffers", counter);
        return this;
    }

    @Step("Добавляем каунтер «Завершённые» = «{counter}»")
    public MockUserStatistics setTotalExpired(int counter) {
        userStatistics.getAsJsonObject(TOTAL_COUNTERS).addProperty("expiredOffers", counter);
        return this;
    }

    @Step("Добавляем каунтер «Проданные» = «{counter}»")
    public MockUserStatistics setTotalSold(int counter) {
        userStatistics.getAsJsonObject(TOTAL_COUNTERS).addProperty("soldOffers", counter);
        return this;
    }

    @Step("Добавляем «viewsStatistics»")
    public MockUserStatistics setViewsStatistics(MockUserStatisticsBarChart barChart) {
        userStatistics.getAsJsonObject(ACTIONS_STATISTICS)
                .add("viewsStatistics", new Gson().toJsonTree(barChart.barChart()).getAsJsonObject());
        return this;
    }

    @Step("Добавляем «favoritesStatistics»")
    public MockUserStatistics setFavoritesStatistics(MockUserStatisticsBarChart barChart) {
        userStatistics.getAsJsonObject(ACTIONS_STATISTICS)
                .add("favoritesStatistics", new Gson().toJsonTree(barChart.barChart()).getAsJsonObject());
        return this;
    }

    @Step("Добавляем «chatInitsStatistics»")
    public MockUserStatistics setChatInitsStatistics(MockUserStatisticsBarChart barChart) {
        userStatistics.getAsJsonObject(ACTIONS_STATISTICS)
                .add("chatInitsStatistics", new Gson().toJsonTree(barChart.barChart()).getAsJsonObject());
        return this;
    }

    @Step("Добавляем «phoneCallsStatistics»")
    public MockUserStatistics setPhoneCallsStatistics(MockUserStatisticsBarChart barChart) {
        userStatistics.getAsJsonObject(ACTIONS_STATISTICS)
                .add("phoneCallsStatistics", new Gson().toJsonTree(barChart.barChart()).getAsJsonObject());
        return this;
    }

    @Step("Добавляем кол-во дней, за которое отображаем статистику = «{startDateOffset}»")
    public MockUserStatistics setStartDateOffset(int startDateOffset) {
        this.startDateOffset = startDateOffset;
        return this;
    }

    public String build() {
        userStatistics.getAsJsonObject(ACTIONS_STATISTICS).getAsJsonObject(PREVIOUS_PERIOD_INFO)
                .addProperty("startDate", formatDate(getDateEarlier(startDateOffset)));
        userStatistics.getAsJsonObject(ACTIONS_STATISTICS).getAsJsonObject(PREVIOUS_PERIOD_INFO)
                .addProperty("endDate", formatDate(getCurrentDate()));
        return userStatistics.toString();
    }

}
