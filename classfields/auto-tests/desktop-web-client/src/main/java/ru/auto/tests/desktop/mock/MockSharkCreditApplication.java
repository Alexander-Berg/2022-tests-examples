package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockSharkCreditApplication {

    public static final String CREDIT_APPLICATION_ACTIVE_EXAMPLE = "mocksConfigurable/shark/CreditApplicationActiveExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject response;

    private MockSharkCreditApplication(String pathToTemplate) {
        this.response = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockSharkCreditApplication creditApplicationActive() {
        return new MockSharkCreditApplication(CREDIT_APPLICATION_ACTIVE_EXAMPLE);
    }

}
