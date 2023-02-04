package ru.yandex.general.myOffers;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.STOPWORD;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.MyOfferSnippet.ACTIVATE;
import static ru.yandex.general.element.MyOfferSnippet.CHAT_SUPPORT;
import static ru.yandex.general.element.MyOfferSnippet.EDIT;
import static ru.yandex.general.mobile.element.MyOfferSnippet.DELETE;
import static ru.yandex.general.mobile.element.MyOfferSnippet.REMOVE_FROM_PUBLICATION;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature("Возможные действия с оффером")
@DisplayName("Возможные действия с оффером")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SnippetAvaliableActionsTest {

    private static final String CHAT_SUPPORT_LINK = "https://yandex.ru/chat#/user/2d001e07-0165-9004-5972-3c2857a2ac80";

    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

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

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        urlSteps.testing().path(MY).path(OFFERS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Редактировать» для активного оффера")
    public void shouldSeeActiveOfferActions() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setActiveOfferAvaliableActions())).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Для активного оффера в попапе действий - кнопка «Снять с продажи»")
    public void shouldSeeActiveOfferActionsInPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setActiveOfferAvaliableActions())).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();

        basePageSteps.onMyOffersPage().popup().button(REMOVE_FROM_PUBLICATION).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Активировать» для неактивного оффера")
    public void shouldSeeInactiveOfferActions() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                        .setInactiveWithReason(SOLD_ON_YANDEX))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().spanLink(ACTIVATE).should(isDisplayed());
        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(not(isDisplayed()));

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Для неактивного оффера в попапе действий - кнопки «Редактировать» и «Удалить»")
    public void shouldSeeInactiveOfferActionsInPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                        .setInactiveWithReason(SOLD_ON_YANDEX))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();

        basePageSteps.onMyOffersPage().popup().link(EDIT).should(isDisplayed());
        basePageSteps.onMyOffersPage().popup().button(DELETE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Написать в поддержку» для забаненного оффера, без возможности редактирования")
    public void shouldSeeBannedOfferActions() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setBannedNoEditOfferAvaliableActions().setBannedWithReason(STOPWORD))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().link(CHAT_SUPPORT).should(hasAttribute(HREF, CHAT_SUPPORT_LINK));
        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().snippetFirst().spanLink(ACTIVATE).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Для забаненного оффера, без возможности редактирования в попапе действий - кнопка «Удалить»")
    public void shouldSeeBannedOfferActionsInPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setBannedNoEditOfferAvaliableActions().setBannedWithReason(STOPWORD))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();

        basePageSteps.onMyOffersPage().popup().button(DELETE).should(isDisplayed());
        basePageSteps.onMyOffersPage().popup().link(EDIT).should(not(isDisplayed()));
    }

}
