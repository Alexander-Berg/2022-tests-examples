package ru.auto.tests.desktop.mock;

import com.google.gson.JsonObject;
import io.qameta.allure.Step;

import static ru.auto.tests.desktop.mock.beans.error.Error.error;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockError {

    @Step("Возвращаем ответ с ошибкой «UNKNOWN_ERROR»")
    public static JsonObject getUnknownError() {
        return getJsonObject(error()
                .setError("UNKNOWN_ERROR")
                .setStatus("ERROR")
                .setDetailedError("Unexpected response from cabinet")
        );
    }

}
