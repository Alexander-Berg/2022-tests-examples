package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.feed.FeedError;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.beans.feed.FeedError.feedError;
import static ru.yandex.general.utils.Utils.getRandomOfferId;

public class MockFeedErrors {

    private static final String FEED_ERRORS_TEMPLATE = "mock/feed/feedErrorsTemplate.json";
    private static final String FEED_ERRORS_AND_WARNINGS = "mock/feed/feedErrorsAndWarnings.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject feedErrors;

    private MockFeedErrors(String pathToTemplate) {
        this.feedErrors = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockFeedErrors mockFeedErrors(String pathToResource) {
        return new MockFeedErrors(pathToResource);
    }

    public static MockFeedErrors feedErrorsTemplate() {
        return new MockFeedErrors(FEED_ERRORS_TEMPLATE);
    }

    public static MockFeedErrors feedErrorsAndWarnings() {
        return new MockFeedErrors(FEED_ERRORS_AND_WARNINGS);
    }

    @Step("Добавляем feedErrors.totalCount = «{count}»")
    public MockFeedErrors setTotalCount(int count) {
        feedErrors.addProperty("totalCount", count);
        return this;
    }

    @Step("Добавляем feedErrors")
    public MockFeedErrors setErrors(FeedError... errors) {
        asList(errors).stream().forEach(error ->
                feedErrors.getAsJsonArray("errors").add(new Gson().toJsonTree(error).getAsJsonObject()));
        return this;
    }

    public static FeedError errorTemplate() {
        return feedError().setErrorType("Crititcal")
                .setExternalOfferId(getRandomOfferId())
                .setTitle("Title")
                .setMessage("Message")
                .setDetailedDescription("Detailed Description");
    }

    public String build() {
        setTotalCount(feedErrors.getAsJsonArray("errors").size());
        return feedErrors.toString();
    }

}
