package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Location.MOSCOW_OBL;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;


@DisplayName("Главная. Геоселектор. Скриншоты")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GeoSelectorCompareTabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String tabName;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Метро"},
                {"Район"},
                {"Шоссе"},
        });
    }

    @Test
    @Owner(KURAU)
    public void compareGeoSelectorPopupTabsTest() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MOSCOW_OBL.getPath()).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().filters().geoButtons().spanLink(METRO).waitUntil(isDisplayed()).click();
        user.onOffersSearchPage().geoSelectorPopup().tab(tabName).click();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(user.onBasePage()
                .geoSelectorPopup().content().waitUntil(isDisplayed()));

        urlSteps.setProductionHost().open();
        user.onOffersSearchPage().filters().geoButtons().spanLink(METRO).waitUntil(isDisplayed()).click();
        user.onOffersSearchPage().geoSelectorPopup().tab(tabName).click();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(user.onBasePage()
                .geoSelectorPopup().content().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
