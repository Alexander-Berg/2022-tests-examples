package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.element.Shortcuts.LOCATION;
import static ru.yandex.realty.element.Shortcuts.NEAR_OFFERS;
import static ru.yandex.realty.element.Shortcuts.PROFITABILITY;
import static ru.yandex.realty.element.Shortcuts.RENT_COST;
import static ru.yandex.realty.element.Shortcuts.RENT_PRICE;
import static ru.yandex.realty.element.Shortcuts.SELL_COST;
import static ru.yandex.realty.element.Shortcuts.SELL_PRICE;
import static ru.yandex.realty.element.Shortcuts.TRANSPORT;
import static ru.yandex.realty.element.Shortcuts.TRANSPORT_ACCESSIBILITY;
import static ru.yandex.realty.element.Shortcuts.YA_DRIVE;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера. Шорткаты")
@Feature(OFFER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShortcutClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private OfferPageSteps offerPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String shortcut;

    @Parameterized.Parameter(1)
    public String mapShortcut;

    @Parameterized.Parameters(name = "{index} Карточка оффера. клик по шорткату - {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {LOCATION, NEAR_OFFERS},
                {TRANSPORT, TRANSPORT_ACCESSIBILITY},
                {SELL_COST, SELL_PRICE},
                {YA_DRIVE, YA_DRIVE},
                {RENT_COST, RENT_PRICE},
                {PROFITABILITY, PROFITABILITY}
        });
    }

    @Before
    public void before() {
        MockOffer offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeScrollAndSelectOffer() {
        offerPageSteps.clickOnElementShouldScroll(() -> offerPageSteps.findShortcut(shortcut));
        offerPageSteps.onOfferCardPage().mapShortcut(mapShortcut).should(isChecked());
    }
}
