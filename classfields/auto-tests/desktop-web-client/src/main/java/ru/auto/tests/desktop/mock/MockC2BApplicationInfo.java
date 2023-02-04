package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockC2BApplicationInfo {

    public static final String C2B_APPLICATION_INFO_EXAMPLE = "mocksConfigurable/poffer/C2bApplicationInfoExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockC2BApplicationInfo(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockC2BApplicationInfo c2bApplicationInfoExample() {
        return new MockC2BApplicationInfo(C2B_APPLICATION_INFO_EXAMPLE);
    }

    @Step("Добавляем can_apply = «{canApply}»")
    public MockC2BApplicationInfo setCanApply(boolean canApply) {
        body.addProperty("can_apply", canApply);
        return this;
    }

}
