package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.REZUME_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Отображение стоимости для покупателя")
@DisplayName("Отображение стоимости для покупателя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardPriceBuyerTest {

    private static final String ID = "12345";

    private MockResponse mockResponse = mockResponse().setCategoriesTemplate().setRegionsTemplate();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена указана")
    public void shouldSeePrice() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).setPrice(3600).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().price().should(hasText("3 600 ₽"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «Бесплатно»")
    public void shouldSeeFreePrice() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).setFreePrice().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().price().should(hasText("Даром"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена не указана")
    public void shouldSeeNoPrice() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).setUnsetPrice().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().price().should(hasText("Не указана"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зарплата указана")
    public void shouldSeeSallary() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard(REZUME_CARD).setIsOwner(false).setSallaryPrice("120000").build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().price().should(hasText("120 000 ₽"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Зарплата не указана")
    public void shouldSeeNoSallary() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard(REZUME_CARD).setIsOwner(false).setUnsetPrice().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().price().should(hasText("Не указана"));
    }

}
