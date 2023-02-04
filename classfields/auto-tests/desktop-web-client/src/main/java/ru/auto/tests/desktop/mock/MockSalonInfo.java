package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockSalonInfo {

    public static final String SALON_LEGACY_UPDATE_REQUEST = "mocksConfigurable/cabinet/SalonInfoPostRequestBody.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockSalonInfo(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockSalonInfo salonInfoRequest() {
        return new MockSalonInfo(SALON_LEGACY_UPDATE_REQUEST);
    }

    @Step("Добавляем для агента client_id = «{clientId}»")
    public MockSalonInfo setClientId(int clientId) {
        body.addProperty("client_id", clientId);
        return this;
    }

}
