package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockReviews {

    public static final String REVIEWS_EXAMPLE = "mocksConfigurable/reviews/ReviewsExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockReviews(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockReviews reviews(String pathToTemplate) {
        return new MockReviews(pathToTemplate);
    }

    public static MockReviews reviewsExample() {
        return reviews(REVIEWS_EXAMPLE);
    }

}
