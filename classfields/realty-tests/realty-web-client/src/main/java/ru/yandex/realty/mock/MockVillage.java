package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.mock.MockSite.SALES_DEPARTMENT;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;

public class MockVillage {

    public static final String VILLAGE_WITHOUT_PHOTO = "mock/village/villageWithoutPhoto.json";
    public static final String VILLAGE_COTTAGE = "mock/village/villageCottage.json";

    private static final String VILLAGE = "village";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject village;

    private MockVillage(String village) {
        this.village = new GsonBuilder().create().fromJson(getResourceAsString(village), JsonObject.class);
    }

    public static MockVillage mockVillage(String pathToVillage) {
        return new MockVillage(pathToVillage);
    }

    @Step("Добавляем id = «{id}»")
    public MockVillage setId(String id) {
        village.getAsJsonObject(VILLAGE).addProperty("id", id);
        return this;
    }

    @Step("Добавляем village.deliveryDates.status = «{status}»")
    public MockVillage setDeliveryDatesStatus(String status) {
        village.getAsJsonObject(VILLAGE).getAsJsonArray("deliveryDates").forEach(
                o -> o.getAsJsonObject().addProperty("status", status));
        return this;
    }

    @Step("Очищаем offerStats.primaryPrices")
    public MockVillage clearPrices() {
        village.getAsJsonObject(VILLAGE).getAsJsonObject("offerStats").remove("primaryPrices");
        return this;
    }

    @Step("Добавляем к моку salesDepartment")
    public MockVillage setSalesDepartment() {
        JsonArray array = new JsonArray();
        array.add(getObjectFromJson(JsonObject.class, SALES_DEPARTMENT));
        village.getAsJsonObject(VILLAGE).add("salesDepartments", array);
        return this;
    }

    @Step("Добавляем name = «{name}» в salesDepartment")
    public MockVillage setSalesDepartmentName(String name) {
        village.getAsJsonObject(VILLAGE).getAsJsonArray("salesDepartments").get(0).getAsJsonObject()
                .addProperty("name", name);
        return this;
    }

    @Step("Добавляем logo в salesDepartment")
    public MockVillage setSalesDepartmentLogo(String logo) {
        village.getAsJsonObject(VILLAGE).getAsJsonArray("salesDepartments").get(0).getAsJsonObject()
                .addProperty("logo", logo);
        return this;
    }

    @Step("Добавляем name = «{name}»")
    public MockVillage setName(String name) {
        village.getAsJsonObject(VILLAGE).addProperty("name", name);
        return this;
    }

    @Step("Очищаем телефоны")
    public MockVillage clearDeveloperPhones() {
        JsonArray array = new JsonArray();
        village.getAsJsonObject(VILLAGE).getAsJsonArray("developers").forEach(o ->
                o.getAsJsonObject().add("phones", array));
        return this;
    }

    @Step("Добавляем villageFeatures.villageClass = «{villageClass}»")
    public MockVillage setVillageClass(String villageClass) {
        village.getAsJsonObject(VILLAGE).getAsJsonObject("villageFeatures").addProperty("villageClass", villageClass);
        return this;
    }

}
