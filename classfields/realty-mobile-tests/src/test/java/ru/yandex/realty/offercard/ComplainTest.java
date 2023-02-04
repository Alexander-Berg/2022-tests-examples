package ru.yandex.realty.offercard;

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
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_GARAGE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class ComplainTest {

    public static final String COMPLAIN = "Пожаловаться на объявление";
    public static final String OTHER_REASON = "Другая причина";
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
        offer = mockOffer(SELL_GARAGE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на «Пожаловаться»")
    public void shouldSeeComplainModal() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().button(COMPLAIN));
        basePageSteps.onOfferCardPage().button(COMPLAIN).click();
        basePageSteps.onOfferCardPage().complainModal().complainOption(OTHER_REASON).click();
        basePageSteps.onOfferCardPage().complainModal().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем модуль «Пожаловаться»")
    public void shouldNotSeeComplainModal() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().button(COMPLAIN));
        basePageSteps.onOfferCardPage().button(COMPLAIN).click();
        basePageSteps.onOfferCardPage().complainModal().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().complainModal().closeCross().click();
        basePageSteps.onOfferCardPage().complainModal().waitUntil(not(isDisplayed()));
    }
}
