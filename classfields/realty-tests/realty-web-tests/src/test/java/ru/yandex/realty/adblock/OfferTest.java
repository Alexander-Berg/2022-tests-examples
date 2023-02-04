package ru.yandex.realty.adblock;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModuleWithAdBlock;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.step.UrlSteps.RGID;

/**
 * @author kantemirov
 */
@DisplayName("Страница оффера с AdBlock'ом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithAdBlock.class)
public class OfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем ссылку хлебных крошек")
    public void shouldSeeRentPageUrl() {
        MockOffer offer = mockOffer(RENT_APARTMENT);
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.findAdblockCookie();
        basePageSteps.onOfferCardPage().breadCrumb("Снять").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(SNYAT).ignoreParam(RGID).shouldNotDiffWithWebDriverUrl();
    }
}
