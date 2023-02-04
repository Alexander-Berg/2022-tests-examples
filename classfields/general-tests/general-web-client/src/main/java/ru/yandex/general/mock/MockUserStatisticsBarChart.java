package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.statistics.GraphItem;

import java.util.ArrayList;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.beans.statistics.GraphItem.graphItem;
import static ru.yandex.general.utils.Utils.getLastDates;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;

public class MockUserStatisticsBarChart {

    private static final String BAR_CHART_TEMPLATE = "mock/userStatistics/barChartTemplate.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject barChart;

    private MockUserStatisticsBarChart(String pathToTemplate) {
        this.barChart = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserStatisticsBarChart statisticsBarChart() {
        return new MockUserStatisticsBarChart(BAR_CHART_TEMPLATE);
    }

    @Step("Добавляем «totalCount» = {count}")
    public MockUserStatisticsBarChart setTotalCount(int count) {
        barChart.addProperty("totalCount", count);
        return this;
    }

    @Step("Добавляем «previousPeriodDifference» = {count}")
    public MockUserStatisticsBarChart setPreviousPeriodDifference(int count) {
        barChart.addProperty("previousPeriodDifference", count);
        return this;
    }

    @Step("Добавляем graph")
    public MockUserStatisticsBarChart setGraph(List<GraphItem> graphItems) {
        JsonArray graph = new JsonArray();
        graphItems.stream().forEach(graphItem ->
                graph.add(new Gson().toJsonTree(graphItem).getAsJsonObject()));
        barChart.add("graph", graph);
        return this;
    }

    public MockUserStatisticsBarChart setGraphForLastDays(int days) {
        List<GraphItem> graphItems = new ArrayList<>();
        getLastDates(days).forEach(day ->
                graphItems.add(graphItem().setDate(day).setCounterValue(getRandomIntInRange(1, 500))));
        setGraph(graphItems);
        return this;
    }

    public MockUserStatisticsBarChart setZeroStatGraph() {
        List<GraphItem> graphItems = new ArrayList<>();
        getLastDates(14).forEach(day ->
                graphItems.add(graphItem().setDate(day).setCounterValue(0)));
        setGraph(graphItems);
        return this;
    }

}
