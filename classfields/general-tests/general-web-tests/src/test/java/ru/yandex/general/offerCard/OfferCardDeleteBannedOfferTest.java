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
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.CardDeleteOffer.cardDeleteOffer;
import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_PHOTO;
import static ru.yandex.general.consts.GeneralFeatures.CHANGE_OFFER_STATUS;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Notifications.OFFER_DELETED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.DELETE;
import static ru.yandex.general.step.AjaxProxySteps.CARD_DELETE_OFFER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(CHANGE_OFFER_STATUS)
@DisplayName("Удаляем забаненный оффер")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardDeleteBannedOfferTest {

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
    public MockCard mockCard;

    @Parameterized.Parameters(name = "{index}. Удаляем оффер забаненный «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"По 1 причине", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(WRONG_OFFER_CATEGORY).setBannedOfferAvaliableActions()},
                {"По 2 причинам", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(WRONG_OFFER_CATEGORY, WRONG_PHOTO).setBannedOfferAvaliableActions()},
                {"По 1 причине без возможности редактирования", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM).setBannedNoEditOfferAvaliableActions()}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setIsOwner(true).setId(ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем оффер забаненный с одной/двумя/без редактирования причинами")
    public void shouldSeeDeleteBannedOffer() {
        basePageSteps.onOfferCardPage().sidebar().rightButton().click();
        basePageSteps.onOfferCardPage().modal().button(DELETE).waitUntil(isDisplayed()).click();
        basePageSteps.onOfferCardPage().popupNotification(OFFER_DELETED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(CARD_DELETE_OFFER).withRequestText(
                cardDeleteOffer().setId(ID)).shouldExist();
    }

}
