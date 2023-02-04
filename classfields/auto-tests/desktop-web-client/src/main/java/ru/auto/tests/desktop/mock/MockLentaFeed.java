package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;


public class MockLentaFeed {

    public static final String FEED_EXAMPLE = "mocksConfigurable/garage/FeedExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    @Getter
    private List<MockLentaFeedPayload> payloads;

    private MockLentaFeed() {
        body = new JsonObject();
        payloads = new ArrayList<>();

        JsonObject pageStatistics = new JsonObject();

        int magazineId = getRandomBetween(9000, 15000);
        pageStatistics.addProperty("from_content_id", format("magazine_%d", magazineId));
        pageStatistics.addProperty("shown_content_count", 3);
        pageStatistics.addProperty("total_count", 1000);

        body.add("payloads", new JsonArray());
        body.add("page_statistics", pageStatistics);
    }

    private MockLentaFeed(String pathToTemplate) {
        payloads = new ArrayList<>();
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockLentaFeed feedTemplate() {
        return new MockLentaFeed();
    }

    public static MockLentaFeed feedExample() {
        return new MockLentaFeed(FEED_EXAMPLE);
    }

    public MockLentaFeed setPayloads(MockLentaFeedPayload... feedPayloads) {
        payloads.addAll(Arrays.asList(feedPayloads));
        return this;
    }

    public MockLentaFeed setId(int index, String id) {
        body.getAsJsonArray("payloads").get(index).getAsJsonObject().addProperty("id", id);
        return this;
    }

    public JsonObject build() {
        if (payloads.size() > 0) {
            JsonArray payloadsList = new JsonArray();
            payloads.forEach(report -> payloadsList.add(report.getBody()));

            body.add("payloads", payloadsList);
        }

        return body;
    }

}
