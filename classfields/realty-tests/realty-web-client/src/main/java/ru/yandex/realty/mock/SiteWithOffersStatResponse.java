package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class SiteWithOffersStatResponse {

    public static final String SITE_WITH_OFFERS_STAT_FOR_NB = "mock/siteWithOffersStatForNB.json";
    public static final String SITE_WITH_OFFERS_STAT_FOR_NB_WITHOUT_PHOTO_JSON =
            "mock/siteWithOffersStatForNBWithoutPhoto.json";
    public static final  String SITE_WITH_OFFERS_STAT_WITH_CALLBACK = "mock/siteWithOffersStatForNBWithCallBack.json";

    private static final String RESPONSE = "response";
    private static final String SITE = "site";
    private static final String DEVELOPERS = "developers";

    private JsonObject template;

    private SiteWithOffersStatResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static SiteWithOffersStatResponse mockSiteWithOffersStat(String pathToTemplate) {
        return new SiteWithOffersStatResponse(pathToTemplate);
    }

    public static SiteWithOffersStatResponse mockSiteWithOffersStatTemplate() {
        return mockSiteWithOffersStat(SITE_WITH_OFFERS_STAT_FOR_NB);
    }

    public static SiteWithOffersStatResponse mockSiteWithOfferStatCallbackTemplate() {
        return mockSiteWithOffersStat(SITE_WITH_OFFERS_STAT_WITH_CALLBACK);
    }

    public static SiteWithOffersStatResponse mockSiteWithOffersStatWithoutPhotoTemplate() {
        return mockSiteWithOffersStat(SITE_WITH_OFFERS_STAT_FOR_NB_WITHOUT_PHOTO_JSON);
    }

    @Step("Добавляем id новостройки = «{id}»")
    public SiteWithOffersStatResponse setNewbuildingId(int id) {
        template.getAsJsonObject(RESPONSE).getAsJsonObject(SITE).addProperty("id", id);
        return this;
    }

    @Step("Добавляем id застройщика = «{id}»")
    public SiteWithOffersStatResponse setDeveloperId(String id) {
        template.getAsJsonObject(RESPONSE).getAsJsonObject(SITE).getAsJsonArray(DEVELOPERS).get(0).getAsJsonObject()
                .addProperty("id", id);
        return this;
    }

    @Step("Добавляем name застройщика = «{name}»")
    public SiteWithOffersStatResponse setDeveloperName(String name) {
        template.getAsJsonObject(RESPONSE).getAsJsonObject(SITE).getAsJsonArray(DEVELOPERS).get(0).getAsJsonObject()
                .addProperty("name", name);
        return this;
    }

    @Step("Удаляем один из типов санузла")
    public SiteWithOffersStatResponse deleteBathroomUnit(int index) {
        template.getAsJsonObject(RESPONSE).getAsJsonObject("filters").getAsJsonArray("bathroomUnit")
                .remove(index);
        return this;
    }

    public String build() {
        return template.toString();
    }

}
