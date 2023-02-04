package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class NewbuildingContactResponse {

    private static final String MOCK_NEWBUILDING_CONTACTS_JSON = "mock/newbuildingContacts.json";
    private static final String MOCK_NEWBUILDING_CONTACTS_PAYED = "mock/newbuildingContactsPayed.json";
    private static final String RESPONSE = "response";
    private static final String SALES_DEPARTMENTS = "salesDepartments";

    private JsonObject template;
    private JsonArray phoneList = new JsonArray();
    private JsonArray phonesWithTag = new JsonArray();

    private NewbuildingContactResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static NewbuildingContactResponse newbuildingContactTemplateFreeJk() {
        return new NewbuildingContactResponse(MOCK_NEWBUILDING_CONTACTS_JSON);
    }

    public static NewbuildingContactResponse newbuildingContactTemplatePayedJk() {
        return new NewbuildingContactResponse(MOCK_NEWBUILDING_CONTACTS_PAYED);
    }

    @Step("Добавляем phone «{phone}»")
    public NewbuildingContactResponse addPhone(String phone) {
        phoneList.add(phone);
        return this;
    }

    @Step("Добавляем phone tag special «{phone}»")
    public NewbuildingContactResponse addSpecialPhone(String phone) {
        JsonObject phoneWithTagSpecial = new JsonObject();
        phoneWithTagSpecial.addProperty("tag", "special-samolet");
        phoneWithTagSpecial.addProperty("phone", phone);
        phonesWithTag.add(phoneWithTagSpecial);
        return this;
    }
//
//    @Step("Добавляем «{phone}» с полем redirectId")
//    public NewbuildingContactResponse addPhoneWithRedirectId(String phone) {
//        JsonObject phoneNumber = new JsonObject();
//        phoneNumber.addProperty("phoneNumber", phone);
//        phoneNumber.addProperty("redirectId", "WEJJFUFU");
//        template.getAsJsonObject("response").getAsJsonArray("phones").add(phoneNumber);
//        return this;
//    }

    public String build() {
        template.getAsJsonObject(RESPONSE).getAsJsonArray(SALES_DEPARTMENTS).get(0).getAsJsonObject()
                .add("phones", phoneList);
        template.getAsJsonObject(RESPONSE).getAsJsonArray(SALES_DEPARTMENTS).get(0).getAsJsonObject()
                .add("phonesWithTag", phonesWithTag);
        return template.toString();
    }
}
