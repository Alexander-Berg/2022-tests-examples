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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockCabinetSnippet;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.ActivateOffers.activateOffers;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.GeneralFeatures.CHANGE_OFFER_STATUS;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Notifications.OFFER_ACTIVATED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.MyOfferSnippet.ACTIVATE;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.AjaxProxySteps.ACTIVATE_OFFERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature(CHANGE_OFFER_STATUS)
@DisplayName("Активируем оффер снятый по разным причинам")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetActivateInactiveOfferTest {

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

    @Parameterized.Parameter()
    public String name;

    @Parameterized.Parameter(1)
    public MockCabinetSnippet cabinetSnippet;

    @Parameterized.Parameter(2)
    public String preset;

    @Parameterized.Parameters(name = "{index}. Активация оффера снятого по причине «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {SOLD_ON_YANDEX.getName(), mockSnippet(BASIC_SNIPPET).setInactiveWithReason(SOLD_ON_YANDEX), "Sold"},
                {SOLD_SOMEWHERE.getName(), mockSnippet(BASIC_SNIPPET).setInactiveWithReason(SOLD_SOMEWHERE), "Sold"},
                {RETHINK.getName(), mockSnippet(BASIC_SNIPPET).setInactiveWithReason(RETHINK), "Expired"},
                {OTHER.getName(), mockSnippet(BASIC_SNIPPET).setInactiveWithReason(OTHER), "Expired"},
                {"Вы долго не выходили на связь", mockSnippet(BASIC_SNIPPET).setCantCallInactiveStatus(), "Expired"}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().setPreset(preset).offers(asList(cabinetSnippet
                        .setInactiveOfferAvaliableActions().setId(ID))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Активируем оффер снятый по разным причинам")
    public void shouldSeeActivateInactiveOffer() {
        basePageSteps.onMyOffersPage().snippetFirst().spanLink(ACTIVATE).click();
        basePageSteps.onMyOffersPage().popupNotification(OFFER_ACTIVATED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(ACTIVATE_OFFERS).withRequestText(
                activateOffers().setOfferIds(asList(ID)).setPreset(preset)).shouldExist();
    }

}
