package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockUserInfo {

    public static final String USER_INFO_EXAMPLE = "mocksConfigurable/user/UserInfo.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockUserInfo(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserInfo mockUserInfo(String pathToTemplate) {
        return new MockUserInfo(pathToTemplate);
    }

    public static MockUserInfo userInfo() {
        return mockUserInfo(USER_INFO_EXAMPLE);
    }

    @Step("Добавляем имя перекупа = «{alias}»")
    public MockUserInfo setAlias(String alias) {
        body.addProperty("alias", alias);
        return this;
    }

    @Step("Добавляем дату регистрации = «{registrationDate}»")
    public MockUserInfo setRegistrationDate(String registrationDate) {
        body.addProperty("registration_date", registrationDate);
        return this;
    }

    public MockUserInfo setOffersCount(String typeName, int activeCount, int inactiveCount) {
        JsonObject item = new JsonObject();
        item.addProperty("active_offers_count", activeCount);
        item.addProperty("inactive_offers_count", inactiveCount);

        body.getAsJsonObject("offers_stats_by_category").add(typeName, item);
        return this;
    }

}
