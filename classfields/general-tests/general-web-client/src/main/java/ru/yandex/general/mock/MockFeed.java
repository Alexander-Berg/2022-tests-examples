package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockFeed {

    private static final String FEED_TEMPLATE = "mock/feed/feedTemplate.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject feed;

    private MockFeed(String pathToTemplate) {
        this.feed = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockFeed mockFeed(String pathToResource) {
        return new MockFeed(pathToResource);
    }

    public static MockFeed feedTemplate() {
        return new MockFeed(FEED_TEMPLATE);
    }

    public String build() {
        return feed.toString();
    }

}
