package ru.auto.tests.cabinet.backonsale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Regions.HIMKI;
import static ru.auto.tests.desktop.consts.Regions.HIMKI_ID;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.SPB;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExample;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExampleOneOffer;
import static ru.auto.tests.desktop.mock.MockComeback.comebackRequest;
import static ru.auto.tests.desktop.mock.MockComeback.getFilter;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.mock.beans.comeback.Comeback.comeback;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кабинет дилера. Снова в продаже. Блок фильтров")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class RegionTest {

    private static final int DEFAULT_RADIUS = 200;
    private static final int NEW_RADIUS = 500;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(comebackRequest()))
                        .withResponseBody(comebackExample().getBody()),
                stub("desktop/GeoSuggest")
        ).create();

        cookieSteps.setCookieForBaseDomain(IS_SHOWING_BACK_ON_SALE_PLACEHOLDER, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем регион")
    public void shouldChooseRegion() {
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().regionInput().sendKeys(HIMKI);
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().suggestItem(HIMKI).waitUntil(isDisplayed()).click();

        mockRule.overwriteStub(4,
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comeback().setFilter(getFilter().setRid(asList(Integer.valueOf(HIMKI_ID)))
                                        .setGeoRadius(DEFAULT_RADIUS))))
                        .withResponseBody(
                                comebackExampleOneOffer().getBody()));

        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().should(hasText(
                format("%s + %d км", HIMKI, DEFAULT_RADIUS)));
        cookieSteps.shouldSeeCookieWithValue(GIDS, HIMKI_ID);
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем 2 региона")
    public void shouldChooseTwoRegion() {
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().selectedRegion(MOSCOW).click();

        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().regionInput().sendKeys(HIMKI);
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().suggestItem(HIMKI).waitUntil(isDisplayed()).click();

        mockRule.overwriteStub(5, stub("desktop/GeoSuggestSpb"));

        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();

        mockRule.overwriteStub(4,
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comeback().setFilter(getFilter()
                                        .setRid(asList(Integer.valueOf(HIMKI_ID), Integer.valueOf(SPB_GEO_ID)))
                                        .setGeoRadius("null"))))
                        .withResponseBody(
                                comebackExampleOneOffer().getBody()));

        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().should(hasText(
                format("%s, %s", HIMKI, SPB)));
        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", HIMKI_ID, SPB_GEO_ID));
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем радиус на выбранном регионе")
    public void shouldChangeRadius() {
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().click();
        basePageSteps.onMainPage().geoSelectPopup().radiusButton(String.valueOf(NEW_RADIUS)).click();

        mockRule.overwriteStub(4,
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comeback().setFilter(getFilter().setRid(asList(Integer.valueOf(MOSCOW_GEO_ID)))
                                        .setGeoRadius(NEW_RADIUS))))
                        .withResponseBody(
                                comebackExampleOneOffer().getBody()));

        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().should(hasText(
                format("%s + %d км", MOSCOW, NEW_RADIUS)));
        cookieSteps.shouldSeeCookieWithValue(GIDS, MOSCOW_GEO_ID);
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Убираем регион")
    public void shouldRemoveRegion() {
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().selectedRegion(MOSCOW).click();

        mockRule.overwriteStub(4,
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comeback().setFilter(getFilter().setRid(asList())
                                        .setGeoRadius("null"))))
                        .withResponseBody(
                                comebackExampleOneOffer().getBody()));

        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
        basePageSteps.onCabinetOnSaleAgainPage().geoSelectButton().should(hasText("Любой регион"));
        cookieSteps.shouldSeeCookieWithValue(GIDS, "");
        urlSteps.shouldNotSeeDiff();
    }

}
