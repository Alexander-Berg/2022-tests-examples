package ru.yandex.realty.redirect;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.page.BasePage.PAGE_NOT_FOUND;
import static ru.yandex.realty.page.BasePage.SERVICE_UNAVAILABLE;

@Link("https://st.yandex-team.ru/VERTISTEST-1689")
@DisplayName("Выбор фильтра -> смотрим урл -> рефреш -> смотрим урл")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RedirectTest {

    private static final String ADDRESS = "Кутузовский проспект";
    private static final String METRO = "Лубянка";
    public static final String ADDRESS_SECOND = "Кремлёвская набережная";
    private static final String METRO_SECOND = "ЗИЛ";
    private static final String DISTRICT = "район Хамовники";
    private static final String SPB_ADDRESS = "проспект Энергетиков";
    private static final String SPB_METRO = "Ладожская";

    private String redirectedFrom;
    private String redirectedTo;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добаляем однокомнатные квартиры")
    public void shouldSeeEkonomKlass() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/studiya/vtorichniy-rynok/";
        redirectedTo = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().checkButton("1");
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убираем студии")
    public void shouldSeeVtotichnyiRynok() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/odnokomnatnaya/vtorichniy-rynok/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().unCheckButton("Студия");
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах балкон")
    public void shouldSeeBalkon() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass-i-s-balkonom/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Балкон");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах кирпичный")
    public void shouldSeeKirpich() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass-i-kirpich/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("Кирпичный"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Кирпичный");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах хрущевка")
    public void shouldSeeKhrushevskiy() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass-i-khrushevskiy/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("Хрущёвка"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Хрущёвка");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах парк")
    public void shouldSeePark() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass-i-s-parkom/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("Парк"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Парк");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах от собственников")
    public void shouldSeeBezPosrednikov() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass-i-bez-posrednikov/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("От собственников"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("От собственников");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах от собственников")
    public void shouldNotSeeBezPosrednikov() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/ekonom-klass-i-s-parkom/";
        redirectedTo = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/studiya,1-komnatnie/" +
                "?newFlat=NO&hasPark=YES&agents=NO";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("От собственников"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("От собственников");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах  «Кутузовский проспект»")
    @Description("Ждёт починки VTF-1682")
    public void shouldSeeStreet() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/st-kutuzovskij-prospekt-172743" +
                "/ekonom-klass/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(ADDRESS);
        basePageSteps.onMainPage().filters().suggest(ADDRESS).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах  «метро Лубянка»")
    public void shouldSeeMetro() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString()
                + "/moskva_i_moskovskaya_oblast/kupit/kvartira/studiya,1-komnatnie/metro-lubyanka/?newFlat=NO";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(METRO);
        basePageSteps.onMainPage().filters().suggest(METRO).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах «метро Лубянка»")
    @Description("Ждёт починки VTF-1682, REALTYFRONT-13698")
    public void shouldNotSeeMetro() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/st-kutuzovskij-prospekt-172743";
        redirectedTo = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/?streetId=172743&metroGeoId=20487";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(METRO);
        basePageSteps.onMainPage().filters().suggest(METRO).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах «Кремлёвская набережная»")
    @Description("Ждёт починки VTF-1682")
    public void shouldSee2Streets() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/st-kutuzovskij-prospekt-172743/" +
                "ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/studiya,1-komnatnie/" +
                "?streetId=172743&streetId=180441&newFlat=NO";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(ADDRESS_SECOND);
        basePageSteps.onMainPage().filters().suggest(ADDRESS_SECOND).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах «метро ЗИЛ»")
    public void shouldSee2Metro() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-lubyanka/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/studiya,1-komnatnie/" +
                "?metroGeoId=20487&metroGeoId=152941&newFlat=NO";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(METRO_SECOND);
        basePageSteps.onMainPage().filters().suggest(METRO_SECOND).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах «район Хамовники»")
    public void shouldSeeDistrict() {
        redirectedFrom = urlSteps.testing().toString() + "/moskva_i_moskovskaya_oblast/kupit/kvartira/ekonom-klass/";
        redirectedTo = urlSteps.testing().toString() +
                "/moskva_i_moskovskaya_oblast/kupit/kvartira/studiya,1-komnatnie/dist-hamovniki-193321/?newFlat=NO";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(DISTRICT);
        basePageSteps.onMainPage().filters().suggest(DISTRICT).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах «проспект Энергетиков»")
    public void shouldSeeStreetSpb() {
        redirectedFrom = urlSteps.testing().toString() + "/sankt-peterburg/kupit/dom/s-uchastkom/";
        redirectedTo = urlSteps.testing().toString() + "/sankt-peterburg/kupit/dom/st-prospekt-ehnergetikov-174484/" +
                "s-uchastkom/";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(SPB_ADDRESS);
        basePageSteps.onMainPage().filters().suggest(SPB_ADDRESS).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем в фильтрах «метро Ладожская»")
    public void shouldNotSeeSpbMetro() {
        redirectedFrom = urlSteps.testing().toString() + "/sankt-peterburg/kupit/dom/s-uchastkom/";
        redirectedTo = urlSteps.testing().toString() + "/sankt-peterburg_i_leningradskaya_oblast/kupit/dom/metro-ladozhskaya/?lotAreaMin=6";
        urlSteps.fromUri(redirectedFrom).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(SPB_METRO);
        basePageSteps.onMainPage().filters().suggest(SPB_METRO).waitUntil(isDisplayed()).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        shouldSeeUrlAndRefresh();
    }

    private void shouldSeeUrlAndRefresh() {
        urlSteps.fromUri(redirectedTo).shouldNotDiffWithWebDriverUrl();
        basePageSteps.refresh();
        basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND).should(not(isDisplayed()));
        basePageSteps.onBasePage().errorPage(SERVICE_UNAVAILABLE).should(not(isDisplayed()));
        urlSteps.fromUri(redirectedTo).shouldNotDiffWithWebDriverUrl();
    }
}
