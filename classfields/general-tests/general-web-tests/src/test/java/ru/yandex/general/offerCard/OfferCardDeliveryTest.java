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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.Sidebar.DELIVERY_RUSSIA;
import static ru.yandex.general.element.Sidebar.DELIVERY_TAXI;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Доставка")
@DisplayName("Доставка")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardDeliveryTest {

    private static final String ID = "12345";

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
    @DisplayName("Отображение доставки курьером")
    public void shouldSeeCourierDelivery() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setSendByCourier(true).setSendWithinRussia(false)
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().deliveryBadge(DELIVERY_TAXI).should(isDisplayed());
        basePageSteps.onOfferCardPage().sidebar().deliveryBadge(DELIVERY_RUSSIA).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение доставки по России")
    public void shouldSeeRussiaDelivery() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setSendWithinRussia(true).setSendByCourier(false)
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().deliveryBadge(DELIVERY_RUSSIA).should(isDisplayed());
        basePageSteps.onOfferCardPage().sidebar().deliveryBadge(DELIVERY_TAXI).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение доставки курьером и по России")
    public void shouldSeeCourierAndRussiaDelivery() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setSendWithinRussia(true).setSendByCourier(true)
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().deliveryBadge(DELIVERY_TAXI).should(isDisplayed());
        basePageSteps.onOfferCardPage().sidebar().deliveryBadge(DELIVERY_RUSSIA).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет бейджей доставки без доставки")
    public void shouldNotSeeDeliveryBadges() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setSendWithinRussia(false).setSendByCourier(false)
                        .setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().deliveryBadge().should(not(isDisplayed()));
    }

}
