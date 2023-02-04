package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.IsSelectedMatcher.isSelected;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@Issue("VERTISTEST-1351")
@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardHeatMapTest {

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

    @Parameterized.Parameter
    public String shortcut;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Инфраструктура"},
                {"Цена аренды"},
                {"Цена продажи"},
                {"Прогноз окупаемости"},
                {"Транспортная доступность"},
        });
    }

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем по карточке тепловой карты")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeHeatMap() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().cardHeatMap(shortcut));
        basePageSteps.onOfferCardPage().cardHeatMap(shortcut).click();
        basePageSteps.onOfferCardPage().heatMapShortcut(shortcut).should(isSelected());
    }
}
