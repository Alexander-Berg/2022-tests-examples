package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.consts.CardStatus;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.CardHideOffer.cardHideOffer;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.GeneralFeatures.CHANGE_OFFER_STATUS;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Notifications.OFFER_NOT_ACTUAL;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.AjaxProxySteps.CARD_HIDE_OFFER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(CHANGE_OFFER_STATUS)
@DisplayName("Снимаем с продажи оффер")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardDeactivateTest {

    private static final String ID = "12345";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public CardStatus.CardDeactivateStatuses deactivateStatus;

    @Parameterized.Parameters(name = "{index}. Оффер снимаем по причине «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {SOLD_ON_YANDEX.getName(), SOLD_ON_YANDEX},
                {SOLD_SOMEWHERE.getName(), SOLD_SOMEWHERE},
                {RETHINK.getName(), RETHINK},
                {OTHER.getName(), OTHER}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(true).setId(ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена статуса оффера при снятии с публикации по разным причинам")
    public void shouldSeeDeactivateOffer() {
        basePageSteps.onOfferCardPage().cardAction().waitUntil(isDisplayed()).click();
        basePageSteps.onOfferCardPage().popup().spanLink(deactivateStatus.getName()).click();
        basePageSteps.onOfferCardPage().popupNotification(OFFER_NOT_ACTUAL).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(CARD_HIDE_OFFER).withRequestText(
                cardHideOffer().setId(ID).setRecallReasons(deactivateStatus.getMockValue())).shouldExist();
    }

}
