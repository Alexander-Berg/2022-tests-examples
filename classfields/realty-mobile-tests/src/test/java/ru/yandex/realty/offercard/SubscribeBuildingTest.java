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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.getRandomUserRequestBody;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class SubscribeBuildingTest {

    public static final String EMAIL = "Электронная почта";
    public static final String SUBSCRIBE = "Подписаться";
    private MockOffer offer;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим пустое поле и замьюченную кнопку")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeSubscribeEmptyField() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().input(EMAIL));
        basePageSteps.onOfferCardPage().input(EMAIL).should(hasValue(""));
        basePageSteps.onOfferCardPage().button(SUBSCRIBE).should(isDisabled());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим заполненное поле и активную кнопку")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeSubscribeNotEmptyField() {
        String email = getRandomEmail();
        apiSteps.createVos2Account(account, getRandomUserRequestBody(account.getId(),
                OWNER.getValue()).withEmail(email));
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().input(EMAIL));
        basePageSteps.onOfferCardPage().input(EMAIL).should(hasValue(email));
        basePageSteps.onOfferCardPage().button(SUBSCRIBE).waitUntil(not(isDisabled()));
    }
}
