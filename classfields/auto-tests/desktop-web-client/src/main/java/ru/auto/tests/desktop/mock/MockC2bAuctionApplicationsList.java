package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockC2bAuctionApplicationsList {

    private static final String APLLICATION_LIST_TEMPLATE = "mocksConfigurable/buyout/C2bAuctionApplicationListResponseBody.json";

    private static final String APPLICATIONS = "applications";

    @Getter
    @Setter
    private JsonObject body;

    @Getter
    private List<MockC2bAuctionApplication> applications;

    private MockC2bAuctionApplicationsList(String pathToTemplate) {
        applications = new ArrayList<>();
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockC2bAuctionApplicationsList userApplicationsResponse() {
        return new MockC2bAuctionApplicationsList(APLLICATION_LIST_TEMPLATE);
    }

    public MockC2bAuctionApplicationsList setApplications(MockC2bAuctionApplication... userApplications) {
        applications.addAll(Arrays.asList(userApplications));
        return this;
    }

    public JsonObject build() {
        if (applications.size() > 0) {
            JsonArray applicationsList = new JsonArray();
            applications.forEach(report -> applicationsList.add(report.getBody()));

            body.add(APPLICATIONS, applicationsList);
            body.getAsJsonObject("pagination").addProperty("total_count", applications.size());
        }
        return body;
    }
}
