package ru.auto.tests.desktop.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;

public class MockC2BApplicationValidate {

    @Getter
    private final JsonObject body = new JsonObject();

    public static MockC2BApplicationValidate c2bApplicationValidateResponse() {
        return new MockC2BApplicationValidate();
    }

    public MockC2BApplicationValidate addErrors(Error... errors) {
        JsonArray validationErrors;
        if (body.getAsJsonArray("validation_errors") == null) {
            validationErrors = new JsonArray();
        } else {
            validationErrors = body.getAsJsonArray("validation_errors");
        }

        for (Error error : errors) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error_code", error.getErrorCode());
            errorJson.addProperty("description", error.getDescription());
            errorJson.addProperty("field", error.getField());

            validationErrors.add(errorJson);
        }

        body.add("validation_errors", validationErrors);

        return this;
    }

    public enum Error {
        COLOR("notexist.color", "Цвет не указан", "color"),
        MILEAGE("notexist.mileage", "Пробег не указан", "mileage"),
        LICENSE_PLATE("required.license_plate", "Укажите госномер", "licensePlate"),
        OWNERS_NUMBER("notexist.owners", "Количество владельцев по ПТС не указано", "ownersNumber");

        @Getter
        private final String errorCode;
        @Getter
        private final String description;
        @Getter
        private final String field;

        Error(String errorCode, String description, String field) {
            this.errorCode = errorCode;
            this.description = description;
            this.field = field;
        }
    }
}
