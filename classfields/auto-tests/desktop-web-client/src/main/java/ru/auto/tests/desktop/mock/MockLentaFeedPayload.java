package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.utils.Utils.getISO8601Date;

public class MockLentaFeedPayload {

    public static final String ARTICLE_EXAMPLE = "mocksConfigurable/garage/FeedArticleExample.json";
    public static final String REVIEW_EXAMPLE = "mocksConfigurable/garage/FeedReviewExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockLentaFeedPayload(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockLentaFeedPayload feedPayload(String pathToTemplate) {
        return new MockLentaFeedPayload(pathToTemplate);
    }

    public static MockLentaFeedPayload articleExample() {
        return feedPayload(ARTICLE_EXAMPLE);
    }

    public static MockLentaFeedPayload reviewExample() {
        return feedPayload(REVIEW_EXAMPLE);
    }

    @Step("Добавляем title = «{title}» к моку айтема ленты")
    public MockLentaFeedPayload setTitle(String title) {
        body.addProperty("title", title);
        return this;
    }

    @Step("Добавляем url = «{url}» к моку айтема ленты")
    public MockLentaFeedPayload setUrl(String url) {
        body.addProperty("url", url);
        return this;
    }

    @Step("Добавляем created дату = «{daysCountSinceToday}» дней от сегодня к моку айтема ленты")
    public MockLentaFeedPayload setCreatedDate(int daysCountSinceToday) {
        body.addProperty("created", getISO8601Date(daysCountSinceToday));
        return this;
    }

}
