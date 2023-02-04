package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCreditApplicationCreateRequest {

    public static final String CREDIT_APPLICATION_CREATE_REQUEST = "mocksConfigurable/credit/CreditApplicationCreateRequest.json";

    private static final String REQUIREMENTS = "requirements";

    @Getter
    @Setter
    private JsonObject body;

    private MockCreditApplicationCreateRequest(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCreditApplicationCreateRequest creditApplicationCreateRequest() {
        return new MockCreditApplicationCreateRequest(CREDIT_APPLICATION_CREATE_REQUEST);
    }

    @Step("Добавляем requirements.max_amount = «{maxAmount}» в мок запроса создания кредитной заявки")
    public MockCreditApplicationCreateRequest setMaxAmount(int maxAmount) {
        body.getAsJsonObject(REQUIREMENTS).addProperty("max_amount", maxAmount);
        return this;
    }

    @Step("Добавляем requirements.term_months = «{termMonths}» в мок запроса создания кредитной заявки")
    public MockCreditApplicationCreateRequest setTermMonths(int termMonths) {
        body.getAsJsonObject(REQUIREMENTS).addProperty("term_months", termMonths);
        return this;
    }

}
