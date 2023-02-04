package ru.yandex.realty.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.realty.beans.developerSearchQuery.DeveloperSearchQueryResponse;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.mock.MockSite.createMoscowSite;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;

public class OfferWithSiteSearchResponse {

    private static final String RESPONSE = "response";
    private static final String SEARCH_QUERY = "searchQuery";

    public static final String OFFER_WITH_SITE_SEARCH_TEMPLATE = "mock/offerWithSiteSearchTemplate.json";
    public static final String DEVELOPER = "mock/site/developer.json";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockOffer> offers;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockSite> sites;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockVillage> villages;

    private OfferWithSiteSearchResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static OfferWithSiteSearchResponse offerWithSiteSearchTemplate() {
        return new OfferWithSiteSearchResponse(OFFER_WITH_SITE_SEARCH_TEMPLATE);
    }

    @Step("Добавляем developers")
    public OfferWithSiteSearchResponse setDeveloper() {
        template.getAsJsonObject(RESPONSE).getAsJsonObject(SEARCH_QUERY)
                .add("developers", getObjectFromJson(JsonArray.class, DEVELOPER));
        return this;
    }

    @Step("Добавляем developers в searchQuery")
    public OfferWithSiteSearchResponse setDeveloper(DeveloperSearchQueryResponse developer) {
        JsonArray array = new JsonArray();
        array.add(new Gson().toJsonTree(developer).getAsJsonObject());
        template.getAsJsonObject(RESPONSE).getAsJsonObject(SEARCH_QUERY).add("developers", array);
        return this;
    }

    @Step("Добавляем «{count}» новостроек в Москве")
    public OfferWithSiteSearchResponse addMoscowSites(int count) {
        List<MockSite> siteList = newArrayList();
        for (int i = 0; i < count; i++) {
            siteList.add(createMoscowSite());
        }
        sites(siteList);
        return this;
    }

    public String build() {
        JsonArray array = new JsonArray();
        offers.forEach(o -> array.add(o.getOffer()));
        template.getAsJsonObject(RESPONSE).getAsJsonObject("offers").add("items", array);
        return template.toString();
    }

    public String buildSite() {
        JsonArray array = new JsonArray();
        sites.forEach(o -> array.add(o.getSite()));
        template.getAsJsonObject(RESPONSE).getAsJsonObject("sites").add("items", array);
        return template.toString();
    }

    public String buildVillage() {
        JsonArray array = new JsonArray();
        villages.forEach(o -> array.add(o.getVillage()));
        template.getAsJsonObject(RESPONSE).add("villages", new JsonObject());
        template.getAsJsonObject(RESPONSE).getAsJsonObject("villages").addProperty("total", 1);
        template.getAsJsonObject(RESPONSE).getAsJsonObject("villages").add("items", array);
        return template.toString();
    }
}
