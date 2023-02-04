package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockC2bAuctionApply {

    public static final String C2B_AUCTION= "mocksConfigurable/buyout/C2bAuctionCanApplyResponseBody.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockC2bAuctionApply (String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockC2bAuctionApply c2bAuctionApplyResponse() {
        return new MockC2bAuctionApply(C2B_AUCTION);
    }

    @Step("Добавляем статус заявки")
    public  MockC2bAuctionApply setAuctionApply(boolean status) {
        body.addProperty("can_apply", status);
        return this;
    }
}
