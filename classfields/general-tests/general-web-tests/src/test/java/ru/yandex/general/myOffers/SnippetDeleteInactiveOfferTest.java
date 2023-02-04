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
import ru.yandex.general.consts.CardStatus;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.HideOffers.hideOffers;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
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
@DisplayName("Удаляем оффер снятый по разным причинам")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetDeleteInactiveOfferTest {

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

    @Parameterized.Parameter(2)
    public String preset;

    @Parameterized.Parameters(name = "{index}. Оффер снимаем по причине «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {SOLD_ON_YANDEX.getName(), SOLD_ON_YANDEX, "Sold"},
                {SOLD_SOMEWHERE.getName(), SOLD_SOMEWHERE, "Sold"},
                {RETHINK.getName(), RETHINK, "Expired"},
                {OTHER.getName(), OTHER, "Expired"}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().setPreset(preset).offers(asList(
                        mockSnippet(BASIC_SNIPPET).setId(ID).setInactiveWithReason(deactivateStatus)
                                .setInactiveOfferAvaliableActions())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем оффер снятый по разным причинам")
    public void shouldSeeDeleteDeactivatedOffer() {
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();
        basePageSteps.onMyOffersPage().modal().button(DELETE).waitUntil(isDisplayed()).click();
        basePageSteps.onMyOffersPage().popupNotification(OFFER_DELETED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(DELETE_OFFERS).withRequestText(
                hideOffers().setOfferIds(asList(ID)).setPreset(preset)).shouldExist();
    }

}
