package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModuleWithoutServiceUnavailable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MAGADAN;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SHOW_SIMILAR_URL_PARAM;

@Link("https://st.yandex-team.ru/VERTISTEST-1637")
@Feature(AMP_FEATURE)
@DisplayName("amp. Пустая выдача")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutServiceUnavailable.class)
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
    public void beforeAmp() {
        compareSteps.resize(375, 5000);
        urlSteps.testing().path(AMP).path(MAGADAN).path(KUPIT).path(KVARTIRA).queryParam(PRICE_MIN_URL_PARAM, PRICE)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE).queryParam(SHOW_SIMILAR_URL_PARAM, "NO").open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Пустая выдача")
    public void shouldSeeSearchNotFoundAmp() {
        basePageSteps.onAmpSaleAdsPage().searchNotFound().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим скриншот пустой выдачи")
    public void shouldSeeSearchNotFoundScreenshotAmp() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onAmpSaleAdsPage().searchNotFound());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onAmpSaleAdsPage().searchNotFound());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
