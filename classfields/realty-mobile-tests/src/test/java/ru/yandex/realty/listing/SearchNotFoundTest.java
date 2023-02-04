package ru.yandex.realty.listing;

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
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MAGADAN;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SHOW_SIMILAR_URL_PARAM;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Пустая выдача")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SearchNotFoundTest {

    private static final String PRICE = "96756843";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(375, 5000);
        urlSteps.testing().path(MAGADAN).path(KUPIT).path(KVARTIRA).queryParam(PRICE_MIN_URL_PARAM, PRICE)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE).queryParam(SHOW_SIMILAR_URL_PARAM, "NO").open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пустая выдача")
    public void shouldSeeSearchNotFound() {
        basePageSteps.onMobileSaleAdsPage().searchNotFound().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот пустой выдачи")
    public void shouldSeeSearchNotFoundScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileSaleAdsPage().searchNotFound());

        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileSaleAdsPage().searchNotFound());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
