package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDealerSettings {

    public static final String DEALER_SETTING_REQUEST_BODY = "mocksConfigurable/cabinet/DealerSettingsPutRequestBody.json";

    public static final String PROPERTY_AUTO_ACTIVATE_CARS_OFFERS = "auto_activate_cars_offers";
    public static final String PROPERTY_AUTO_ACTIVATE_COMMERCIAL_OFFERS = "auto_activate_commercial_offers";
    public static final String PROPERTY_AUTO_ACTIVATE_MOTO_OFFERS = "auto_activate_moto_offers";
    public static final String PROPERTY_ALLOW_PHOTO_REORDER = "allow_photo_reorder";
    public static final String PROPERTY_HIDE_LICENSE_PLATE = "hide_license_plate";
    public static final String PROPERTY_CHAT_ENABLED = "chat_enabled";
    public static final String PROPERTY_OVERDRAFT_ENABLED = "overdraft_enabled";
    public static final String PROPERTY_OVERDRAFT_BALANCE_PERSON_ID = "overdraft_balance_person_id";

    private static final String PROPERTIES = "properties";


    private final JsonObject requestBody;

    private MockDealerSettings(String pathToTemplate) {
        this.requestBody = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockDealerSettings mockDealerSettings() {
        return new MockDealerSettings(DEALER_SETTING_REQUEST_BODY);
    }

    @Step("В запросе выставляем chat_enabled = «{isChatEnabled}»")
    public MockDealerSettings setChatEnabledRequest(boolean isChatEnabled) {
        requestBody.getAsJsonObject(PROPERTIES).addProperty(PROPERTY_CHAT_ENABLED, isChatEnabled);
        return this;
    }

    @Step("В запросе выставляем overdraft_enabled = «{isOverdraftEbabled}»")
    public MockDealerSettings setOverdraftEnabledRequest(boolean isOverdraftEnabled) {
        requestBody.getAsJsonObject(PROPERTIES).addProperty(PROPERTY_OVERDRAFT_ENABLED, isOverdraftEnabled);
        return this;
    }

    @Step("В запросе выставляем overdraft_balance_person_id = «{balancePersonId}»")
    public MockDealerSettings setOverdraftBalancePersonIdRequest(String balancePersonId) {
        requestBody.getAsJsonObject(PROPERTIES).addProperty(PROPERTY_OVERDRAFT_BALANCE_PERSON_ID, balancePersonId);
        return this;
    }

    @Step("В запросе выставляем properties {propertyName} = «{propertyValue}»")
    public MockDealerSettings setPropertyRequest(String propertyName, boolean propertyValue) {
        requestBody.getAsJsonObject(PROPERTIES).addProperty(propertyName, propertyValue);
        return this;
    }

    public JsonObject getRequestBody() {
        return requestBody;
    }
}
