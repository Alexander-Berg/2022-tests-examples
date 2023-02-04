package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDealerTariff {

    public static final String CALLS_NEW_CARS_TARIFF = "mocksConfigurable/tariffs/TariffCallsNewCars.json";
    public static final String CALLS_USED_CARS_TARIFF = "mocksConfigurable/tariffs/TariffCallsUsedCars.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockDealerTariff(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockDealerTariff tariff(String pathToTemplate) {
        return new MockDealerTariff(pathToTemplate);
    }

    public static MockDealerTariff newCarsCallsTariff() {
        return tariff(CALLS_NEW_CARS_TARIFF);
    }

    public static MockDealerTariff usedCarsCallsTariff() {
        return tariff(CALLS_USED_CARS_TARIFF);
    }

    @Step("Добавляем calls.limits.current_daily.funds = «{dailyLimit}» в мок тарифов")
    public MockDealerTariff setCallsDailyLimit(int dailyLimit) {
        body.getAsJsonObject("calls").getAsJsonObject("limits").getAsJsonObject("current_daily")
                .addProperty("funds", dailyLimit);
        return this;
    }

}
