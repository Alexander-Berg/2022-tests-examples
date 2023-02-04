package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCategories {

    private static final String CATEGORIES = "mock/categories.json";
    private static final String RECOMMENDED_ROOT_CATEGORIES = "mock/recommendedRootCategories.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonArray categories;

    private MockCategories(String pathToTemplate) {
        this.categories = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonArray.class);
    }

    public static MockCategories categoriesTemplate() {
        return new MockCategories(CATEGORIES);
    }

    public static MockCategories recommendedRootCategoriesTemplate() {
        return new MockCategories(RECOMMENDED_ROOT_CATEGORIES);
    }

    public String build() {
        return categories.toString();
    }

}
