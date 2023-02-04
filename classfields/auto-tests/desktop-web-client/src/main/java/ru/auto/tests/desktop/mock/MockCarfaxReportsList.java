package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCarfaxReportsList {

    private static final String CARFAX_REPORTS_TEMPLATE = "mocksConfigurable/carfax/CarfaxReportsListTemplate.json";

    @Getter
    @Setter
    private JsonObject body;

    @Getter
    private List<MockCarfaxReport> reports;

    private MockCarfaxReportsList(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCarfaxReportsList carfaxReportsResponse() {
        return new MockCarfaxReportsList(CARFAX_REPORTS_TEMPLATE);
    }

    public MockCarfaxReportsList setReports(MockCarfaxReport... carfaxReports) {
        reports = new ArrayList<>();
        reports.addAll(Arrays.asList(carfaxReports));
        return this;
    }

    public JsonObject build() {
        reports.forEach(report -> body.getAsJsonArray("reports").add(report.getBody()));
        body.getAsJsonObject("paging").addProperty("total", reports.size());
        return body;
    }

}
