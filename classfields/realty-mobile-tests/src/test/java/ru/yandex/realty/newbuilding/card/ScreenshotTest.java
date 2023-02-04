package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ScreenshotTest {

    private static final String PATH_TO_ZH_K_WITH_CLOSE_SALES = "/malaya-finlyandiya-121364/";
    private static final String ID_ZH_K_WITH_CLOSE_SALES = "121364";
    private static final String PATH_TO_ZH_K_WITHOUT_OFFERS = "/oktyabrskoe-pole/";
    private static final String ZH_K_WITHOUT_OFFERS = "189856";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        basePageSteps.disableAd();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карточки новостройки")
    public void shouldSeeNewBuildingCard() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
        screenshotWithResize();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карточки новостройки с закрытыми продажами")
    public void shouldSeeNewBuildingCardWithCloseSales() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(PATH_TO_ZH_K_WITH_CLOSE_SALES)
                .queryParam("id", ID_ZH_K_WITH_CLOSE_SALES).open();
        screenshotWithResize();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карточки новостройки с открытыми продажами, без офферов")
    public void shouldSeeNewBuildingCardWithoutOffers() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(PATH_TO_ZH_K_WITHOUT_OFFERS)
                .queryParam("id", ZH_K_WITHOUT_OFFERS).open();
        screenshotWithResize();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карточки новостройки В регионе ЦЗ")
    public void shouldSeeNewBuildingCzRegion() {
        urlSteps.testing().path("/vyborg/").path(KUPIT).path(NOVOSTROJKA).path("/privokzalnyj-per-6/")
                .queryParam("id", "1721013").open();
        screenshotWithResize();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карточки новостройки в не регионе ЦЗ ")
    public void shouldSeeNewBuildingNotCzRegion() {
        urlSteps.testing().path("/omsk/").path(KUPIT).path(NOVOSTROJKA).path("/regata/")
                .queryParam("id", "658888").open();
        screenshotWithResize();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот карточки новостройки два застройщика - НЕ спец проект ")
    public void shouldSeeNewBuildingTwoDevNotSpecialProject() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path("/nekrasovka-dsk-1-avesta-stroj/")
                .queryParam("id", "85348").open();
        basePageSteps.scrolling(15000, 500);
        int y = basePageSteps.onNewBuildingCardPage().pageRoot().getSize().getHeight();
        basePageSteps.resize(375, y);
        basePageSteps.onNewBuildingCardPage().cardsDevInfo().waitUntil(hasSize(2));
        basePageSteps.onNewBuildingCardPage().similarSitesList().waitUntil(not(exists()));
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().pageRoot());
        basePageSteps.resize(375, 800);
        urlSteps.setMobileProductionHost().open();
        basePageSteps.scrolling(15000, 500);
        basePageSteps.resize(375, y);
        basePageSteps.onNewBuildingCardPage().cardsDevInfo().waitUntil(hasSize(2));
        basePageSteps.onNewBuildingCardPage().similarSitesList().waitUntil(not(exists()));
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private void screenshotWithResize() {
        //скроллим чтобы подгрузилась вся страница
        basePageSteps.scrolling(15000, 500);
        int y = basePageSteps.onNewBuildingCardPage().pageRoot().getSize().getHeight();
        basePageSteps.resize(375, y);
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().pageRoot());
        //возвращаем обратно разрешение иначе галерея новостройки неправильно ресайзится
        basePageSteps.resize(375, 800);
        urlSteps.setMobileProductionHost().open();
        basePageSteps.scrolling(15000, 500);
        basePageSteps.resize(375, y);
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingCardPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
