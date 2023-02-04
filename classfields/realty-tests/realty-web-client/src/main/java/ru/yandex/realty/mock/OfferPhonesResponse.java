package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class OfferPhonesResponse {

    public static final String MOCK_OFFERS_PHONES_JSON = "mock/offers_phones.json";
    public static final String TEST_PHONE = "+71112223344";

    private JsonObject template;

    private OfferPhonesResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static OfferPhonesResponse offersPhonesTemplate() {
        return new OfferPhonesResponse(MOCK_OFFERS_PHONES_JSON);
    }

    @Step("Добавляем «{phone}»")
    public OfferPhonesResponse addPhone(String phone) {
        JsonObject phoneNumber = new JsonObject();
        phoneNumber.addProperty("phoneNumber", phone);
        template.getAsJsonObject("response").getAsJsonArray("phones").add(phoneNumber);
        template.getAsJsonObject("response").getAsJsonArray("contacts").get(0).getAsJsonObject()
                .getAsJsonArray("phones").add(phoneNumber);
        return this;
    }

    @Step("Добавляем «{phone}» с полем redirectId")
    public OfferPhonesResponse addPhoneWithRedirectId(String phone) {
        JsonObject phoneNumber = new JsonObject();
        phoneNumber.addProperty("phoneNumber", phone);
        phoneNumber.addProperty("redirectId", "WEJJFUFU");
        template.getAsJsonObject("response").getAsJsonArray("phones").add(phoneNumber);
        template.getAsJsonObject("response").getAsJsonArray("contacts").get(0).getAsJsonObject()
                .getAsJsonArray("phones").add(phoneNumber);
        template.getAsJsonObject("response").getAsJsonArray("contacts").get(0).getAsJsonObject()
                .addProperty("isRedirectPhones", true);
        return this;
    }

    public String build() {
        return template.toString();
    }
}
