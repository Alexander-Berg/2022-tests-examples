package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class NearMetroTest {

    private MockOffer offer;

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

    @Before
    public void before() {
        offer = mockOffer(RENT_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на кнопку «ещё» в списке ближайших метро")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeMoreNearMetro() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        int initialMetroCount = basePageSteps.onOfferCardPage().nearMetros()
                .filter(AtlasWebElement::isDisplayed).size();
        basePageSteps.scrollToElement(basePageSteps.onOfferCardPage().nearMetrosShowMore());
        basePageSteps.onOfferCardPage().nearMetrosShowMore().click();
        basePageSteps.onOfferCardPage().nearMetros().filter(AtlasWebElement::isDisplayed)
                .should(hasSize(greaterThan(initialMetroCount)));
    }
}
