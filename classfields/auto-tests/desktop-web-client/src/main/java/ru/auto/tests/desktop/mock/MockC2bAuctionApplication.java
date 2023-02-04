package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.consts.AuctionApplicationStatus;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockC2bAuctionApplication {

    public static final String C2B_APPLICATION = "mocksConfigurable/buyout/C2bAuctionApplicationExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockC2bAuctionApplication(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockC2bAuctionApplication c2bAuctionApplication() {
        return new MockC2bAuctionApplication(C2B_APPLICATION);
    }

    @Step("Добавляем статус заявки «{status}»")
    public MockC2bAuctionApplication setStatus(AuctionApplicationStatus.StatusName status) {
        body.addProperty("status", status.getStatus());
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockC2bAuctionApplication setId(String id) {
        body.addProperty("id", id);
        return this;
    }
}
