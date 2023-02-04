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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.VORONEZH;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.PIK;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.step.UrlSteps.REAL_PRODUCTION;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Промо блок в листинге")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PromoItemTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.clearCookie("isAdDisabledTest");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Промо блок в листинге Москва")
    public void shouldSeePromoMoskva() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().promo().waitUntil(isDisplayed()).hover();
        basePageSteps.onMobileSaleAdsPage().promo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(REAL_PRODUCTION).path(PIK).queryParam("section", "catalog").queryParam("pikRgid", "741964")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Промо блок в листинге Воронеж")
    public void shouldSeePromoVoronezh() {
        urlSteps.testing().path(VORONEZH).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().promo().click();

        urlSteps.testing().path(VORONEZH).path(KUPIT).path(NOVOSTROJKA).shouldNotDiffWithWebDriverUrl();
    }
}
