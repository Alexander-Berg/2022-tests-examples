package ru.yandex.realty.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import ru.yandex.realty.beans.developer.DeveloperTab;
import ru.yandex.realty.beans.developer.office.OfficeResponse;
import ru.yandex.realty.beans.developer.slide.SlideResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.beans.developer.office.OfficeResponse.tyumenOffice;
import static ru.yandex.realty.consts.Filters.TYUMENSKAYA_OBLAST;

public class MockDeveloper {

    /*
       ID расширенной карточки застройщика хранится в бункере, со временем протухает
       Как протухнет, берём новый с самой поздней endDate отсюда:
       https://bunker.yandex-team.ru/realty-www/site-special-developers?view=raw
    */
    public static final String ENHANCED_DEV_ID = "650428";
    public static final String ENHANCED_DEV_GEO_ID_PATH = TYUMENSKAYA_OBLAST;
    public static final String BASIC_DEVELOPER = "mock/developer/basicDeveloper.json";
    public static final String ENHANCED_DEVELOPER = "mock/developer/enhancedDeveloper.json";
    public static final String SITES_FOR_DEVELOPER_CARD = "mock/developer/offerWithSiteSearchForDeveloperCard.json";

    private static final String RESPONSE = "response";
    private static final String SLIDERS = "sliders";
    private static final String CUSTOM_TABS = "customTabs";
    private static final String SITE = "site";
    private static final String GEO_STATISTIC = "geoStatistic";
    private static final String CURRENT = "current";
    private static final String PHONES = "phones";
    private static final String OFFICES = "offices";
    private static final String COORDINATES = "coordinates";

    private JsonObject template;

    private MockDeveloper(String developer) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(developer), JsonObject.class);
    }

    public static MockDeveloper mockDeveloper(String pathToDeveloper) {
        return new MockDeveloper(pathToDeveloper);
    }

    public static MockDeveloper mockEnhancedDeveloper() {
        return mockDeveloper(ENHANCED_DEVELOPER).setId(ENHANCED_DEV_ID);
    }

    @Step("Добавляем id = «{id}»")
    public MockDeveloper setId(String id) {
        template.getAsJsonObject(RESPONSE).addProperty("id", id);
        return this;
    }

    @Step("Добавляем табы")
    public MockDeveloper setTabs(DeveloperTab... developerTabs) {
        template.getAsJsonObject(RESPONSE).add(CUSTOM_TABS, new Gson().toJsonTree(asList(developerTabs)));
        return this;
    }

    @Step("Добавляем телефоны")
    public MockDeveloper setPhones(String... phones) {
        template.getAsJsonObject(RESPONSE).add(PHONES, new Gson().toJsonTree(asList(phones)));
        return this;
    }

    @Step("Убираем статистику офферов")
    public MockDeveloper removeOfferStatistic() {
        template.getAsJsonObject(RESPONSE).getAsJsonObject(GEO_STATISTIC).getAsJsonObject(CURRENT).remove("totalOffers");
        template.getAsJsonObject(RESPONSE).getAsJsonObject(GEO_STATISTIC).getAsJsonObject(CURRENT).remove("minPrice");
        return this;
    }

    @Step("Добавляем слайды")
    public MockDeveloper setSlides(SlideResponse... slides) {
        template.getAsJsonObject(RESPONSE).add(SLIDERS, new Gson().toJsonTree(asList(slides)));
        return this;
    }

    @Step("Добавляем офисы")
    public MockDeveloper setOffices(OfficeResponse... offices) {
        template.getAsJsonObject(RESPONSE).add(OFFICES, new Gson().toJsonTree(asList(offices)));
        return this;
    }

    @Step("Добавляем «{count}» офисов")
    public MockDeveloper addOffices(int count) {
        List<OfficeResponse> offices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            offices.add(tyumenOffice());
        }
        setOffices(offices.toArray(new OfficeResponse[offices.size()]));
        return this;
    }

    public String build() {
        return template.toString();
    }

}
