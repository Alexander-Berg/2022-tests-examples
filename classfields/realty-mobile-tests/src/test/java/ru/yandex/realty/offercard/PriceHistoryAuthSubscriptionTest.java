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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mobile.element.offercard.PriceHistoryModal.EMAIL;
import static ru.yandex.realty.mobile.element.offercard.PriceHistoryModal.SUBSCRIBE;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class PriceHistoryAuthSubscriptionTest {

    public static final String BUTTON_DISABLED = "Button_disabled";
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

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        apiSteps.createVos2Account(account, OWNER);
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим заполненное поле почты и включенную кнопку")
    public void shouldSeeFilledEmail() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().priceHistoryButton());
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().priceHistoryButton());
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().button(SUBSCRIBE)
                .should(hasClass(not(containsString(BUTTON_DISABLED))));
        basePageSteps.onOfferCardPage().priceHistoryModal().input(EMAIL)
                .should(hasValue(vos2Adaptor.getVos2Account(account.getId()).getUser().getEmail()));
    }
}
