package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockChat {

    private static final String CHAT_TEMPLATE = "mock/chatTemplate.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject chat;

    private MockChat(String pathToTemplate) {
        this.chat = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockChat chatTemplate() {
        return new MockChat(CHAT_TEMPLATE);
    }

    @Step("Добавляем isNew чата = «{isNew}»")
    public MockChat setIsNew(boolean isNew) {
        chat.addProperty("isNew", isNew);
        return this;
    }

    public String build() {
        return chat.toString();
    }

}
