package ru.yandex.realty.mainpage;

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
import ru.auto.test.api.realty.ApiSearcher;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Ссылки и наличие премиума офферов блока Премиум объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PremiumOffersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private ApiSearcher searcher;

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки и наличие премиума офферов блока «Премиум объявления»")
    public void shouldSeeOfferUrls() {
        String url = urlSteps.testing().path("/offer/").toString();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileMainPage().premiumOffers());
        basePageSteps.onMobileMainPage().premiumOffers().sliderItems().forEach(item -> {
            item.link().should(hasHref(findPattern(String.format("%s\\d*/", url))));
            apiSteps.checkOfferPremiumEnabled(item.offerId());
        });
    }

}
