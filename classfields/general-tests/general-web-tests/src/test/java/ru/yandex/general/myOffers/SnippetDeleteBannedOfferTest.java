package ru.yandex.general.myOffers;

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
import ru.yandex.general.mock.MockCabinetSnippet;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.HideOffers.hideOffers;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_PHOTO;
import static ru.yandex.general.consts.GeneralFeatures.CHANGE_OFFER_STATUS;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Notifications.OFFER_DELETED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.MyOffersPage.DELETE;
import static ru.yandex.general.step.AjaxProxySteps.DELETE_OFFERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature(CHANGE_OFFER_STATUS)
@DisplayName("Удаляем оффер с одним/двумя причинами бана")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetDeleteBannedOfferTest {

    private static final String ID = "12345";
    private static final String BANNED = "Banned";

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
    public MockCabinetSnippet cabinetSnippet;

    @Parameterized.Parameters(name = "{index}. Удаляем оффер забаненный «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"По 1 причине", mockSnippet(BASIC_SNIPPET).setBannedWithReason(WRONG_OFFER_CATEGORY)},
                {"По 2 причинам", mockSnippet(BASIC_SNIPPET).setBannedWithReason(WRONG_OFFER_CATEGORY, WRONG_PHOTO)}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().setPreset(BANNED).offers(asList(
                        cabinetSnippet.setId(ID).setBannedOfferAvaliableActions())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем оффер с одним/двумя причинами бана")
    public void shouldSeeDeleteBannedOffer() {
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();
        basePageSteps.onMyOffersPage().modal().button(DELETE).waitUntil(isDisplayed()).click();
        basePageSteps.onMyOffersPage().popupNotification(OFFER_DELETED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(DELETE_OFFERS).withRequestText(
                hideOffers().setOfferIds(asList(ID)).setPreset(BANNED)).shouldExist();
    }

}
