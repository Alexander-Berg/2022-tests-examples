package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockReview {

    public static final String REVIEW_EXAMPLE = "mocksConfigurable/reviews/ReviewTesla.json";

    private static final String REVIEW = "review";
    private static final String ITEM = "item";
    private static final String AUTO = "auto";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockReview(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockReview review(String pathToTemplate) {
        return new MockReview(pathToTemplate);
    }

    public static MockReview reviewExample() {
        return review(REVIEW_EXAMPLE);
    }

    public MockReview setId(String id) {
        body.getAsJsonObject(REVIEW).addProperty("id", id);
        return this;
    }

    @Step("Добавляем engineType = «{engineType}» в моке отзыва")
    public MockReview setEngineType(String engineType) {
        body.getAsJsonObject(REVIEW).getAsJsonObject(ITEM).getAsJsonObject(AUTO)
                .addProperty("engine_type", engineType);
        body.getAsJsonObject(REVIEW).getAsJsonObject(ITEM).getAsJsonObject(AUTO).getAsJsonObject("tech_param")
                .addProperty("engine_type", engineType);
        return this;
    }

}
