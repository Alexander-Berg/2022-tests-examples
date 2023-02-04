package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCatalogEntity {

    public static final String TESLA_MODEL_3 = "mocksConfigurable/catalog/TeslaModel3.json";

    public static final String ENTITIES = "entities";
    public static final String TECH_PARAM = "tech_param";

    @Getter
    @Setter
    private JsonObject body;

    private MockCatalogEntity(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCatalogEntity mockCatalogEntity(String pathToTemplate) {
        return new MockCatalogEntity(pathToTemplate);
    }

    @Step("Добавляем markName = «{markName}»")
    public MockCatalogEntity setMarkName(String markName) {
        body.getAsJsonObject("mark_info").addProperty("name", markName);
        return this;
    }

    @Step("Добавляем modelName = «{modelName}»")
    public MockCatalogEntity setModelName(String modelName) {
        body.getAsJsonObject("model_info").addProperty("name", modelName);
        return this;
    }

    @Step("Добавляем superGen = «{superGen}»")
    public MockCatalogEntity setSuperGenName(String superGen) {
        body.getAsJsonObject("super_gen").addProperty("name", superGen);
        return this;
    }

    @Step("Добавляем techParam = «{techParam}»")
    public MockCatalogEntity setTechParamName(String techParam) {
        body.getAsJsonObject(TECH_PARAM).addProperty("nameplate", techParam);
        return this;
    }

    @Step("Добавляем landingDescription = «{landingDescription}»")
    public MockCatalogEntity setLandingDescription(String landingDescription) {
        body.getAsJsonObject("configuration").addProperty("landing_description", landingDescription);
        return this;
    }

    @Step("Добавляем запас хода = «{electricRange}»")
    public MockCatalogEntity setElectricRange(int electricRange) {
        body.getAsJsonObject(TECH_PARAM).addProperty("electric_range", electricRange);
        return this;
    }

    @Step("Добавляем мощность = «{power}» л.с.")
    public MockCatalogEntity setPower(int power) {
        body.getAsJsonObject(TECH_PARAM).addProperty("power", power);
        return this;
    }

    @Step("Добавляем мощность = «{powerKvt}» кВт")
    public MockCatalogEntity setPowerKvt(int powerKvt) {
        body.getAsJsonObject(TECH_PARAM).addProperty("power_kvt", powerKvt);
        return this;
    }

    @Step("Добавляем разгон = «{acceleration}»")
    public MockCatalogEntity setAcceleration(double acceleration) {
        body.getAsJsonObject(TECH_PARAM).addProperty("acceleration", String.format("%.1f", acceleration));
        return this;
    }

    @Step("Добавляем время зарядки = «{chargeTime}»")
    public MockCatalogEntity setChargeTime(int chargeTime) {
        body.getAsJsonObject(TECH_PARAM).addProperty("charge_time", chargeTime);
        return this;
    }

    public JsonObject build() {
        JsonObject response = new JsonObject();
        JsonArray entities = new JsonArray();

        entities.add(body);
        response.add(ENTITIES, entities);

        return response;
    }

}
