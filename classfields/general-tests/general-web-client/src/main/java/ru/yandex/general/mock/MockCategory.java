package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCategory {

    private static final String CATEGORY_PLANSHET = "mock/currentDraft/category_planshet.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject category;

    private MockCategory(String pathToTemplate) {
        this.category = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCategory categoryTemplate() {
        return new MockCategory(CATEGORY_PLANSHET);
    }

    public String build() {
        return category.toString();
    }

}
