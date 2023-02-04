package ru.yandex.realty.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.realty.beans.DeveloperSiteResponse;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;

public class MockSite {

    public static final String SITE_TEMPLATE = "mock/site/siteTemplate.json";
    public static final String MOSCOW_SITE = "mock/site/moscowSite.json";
    public static final String SITE_WITHOUT_PHOTO = "mock/site/siteWithoutPhoto.json";
    public static final String SITE_WITHOUT_SUBWAY = "mock/site/siteWithoutSubway.json";
    public static final String SALES_DEPARTMENT = "mock/site/salesDepartment.json";

    private static final double MOSCOW_LATITUDE = 55.753960;
    private static final double MOSCOW_LONGITUDE = 37.620393;

    private static final String LOCATION = "location";
    private static final String POINT = "point";
    private static final String DEVELOPERS = "developers";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject site;

    private MockSite(String site) {
        this.site = new GsonBuilder().create().fromJson(getResourceAsString(site), JsonObject.class);
    }

    public static MockSite mockSite(String pathToOffer) {
        return new MockSite(pathToOffer);
    }

    @Step("Добавляем к моку salesDepartment")
    public MockSite setSalesDepartment() {
        site.add("salesDepartment", getObjectFromJson(JsonObject.class, SALES_DEPARTMENT));
        return this;
    }

    @Step("Добавляем name = «{name}» в salesDepartment")
    public MockSite setSalesDepartmentName(String name) {
        site.getAsJsonObject("salesDepartment").addProperty("name", name);
        return this;
    }

    @Step("Добавляем logo в salesDepartment")
    public MockSite setSalesDepartmentLogo(String logo) {
        site.getAsJsonObject("salesDepartment").addProperty("logo", logo);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockSite setId(int id) {
        site.addProperty("id", id);
        return this;
    }

    @Step("Добавляем developers")
    public MockSite setDevelopers(DeveloperSiteResponse... developers) {
        site.remove("developers");
        site.add("developers", new Gson().toJsonTree(asList(developers)));
        return this;
    }

    @Step("Добавляем state = «{state}»")
    public MockSite setState(String state) {
        site.addProperty("state", state);
        return this;
    }

    @Step("Добавляем finishedApartments = «{finishedApartments}»")
    public MockSite setFinishedApartments(boolean finishedApartments) {
        site.addProperty("finishedApartments", finishedApartments);
        return this;
    }

    @Step("Добавляем buildingClass = «{buildingClass}»")
    public MockSite setBuildingClass(String buildingClass) {
        site.addProperty("buildingClass", buildingClass);
        return this;
    }

    @Step("Очищаем цены")
    public MockSite clearPrices() {
        site.getAsJsonObject("price").remove("from");
        site.getAsJsonObject("price").remove("to");
        site.getAsJsonObject("price").remove("minPricePerMeter");
        site.getAsJsonObject("price").remove("maxPricePerMeter");
        return this;
    }

    @Step("Очищаем телефоны")
    public MockSite clearDeveloperPhones() {
        JsonArray array = new JsonArray();
        site.getAsJsonArray("developers").forEach(o -> o.getAsJsonObject().add("phones", array));
        return this;
    }

    @Step("Добавляем fullName = «{fullName}»")
    public MockSite setFullName(String fullName) {
        site.addProperty("fullName", fullName);
        return this;
    }

    @Step("Добавляем координаты latitude = «{latitude}»")
    public MockSite setLatitude(double latitude) {
        site.getAsJsonObject(LOCATION).getAsJsonObject(POINT).addProperty("latitude", latitude);
        return this;
    }

    @Step("Добавляем координаты longitude = «{longitude}»")
    public MockSite setLongitude(double longitude) {
        site.getAsJsonObject(LOCATION).getAsJsonObject(POINT).addProperty("longitude", longitude);
        return this;
    }

    @Step("Создаем новостройку в Москве")
    public static MockSite createMoscowSite() {
        return mockSite(MOSCOW_SITE).setLatitude(MOSCOW_LATITUDE + Math.random() * 0.02)
                .setLongitude(MOSCOW_LONGITUDE + Math.random() * 0.02);
    }

    @Step("Добавляем id застройщика = «{id}»")
    public MockSite setDeveloperId(String id) {
        site.getAsJsonArray(DEVELOPERS).get(0).getAsJsonObject()
                .addProperty("id", id);
        return this;
    }

    @Step("Добавляем name застройщика = «{name}»")
    public MockSite setDeveloperName(String name) {
        site.getAsJsonArray(DEVELOPERS).get(0).getAsJsonObject()
                .addProperty("name", name);
        return this;
    }

}
