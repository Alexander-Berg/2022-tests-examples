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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.STOPWORD;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.consts.QueryParams.ROOT_CATEGORY_ID_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.MyOfferSnippet.ACTIVATE;
import static ru.yandex.general.element.MyOfferSnippet.EDIT;
import static ru.yandex.general.element.MyOfferSnippet.KNOW_REASON;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature("Возможные действия с оффером")
@DisplayName("Возможные действия с оффером")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SnippetAvaliableActionsTest {

    private static final String URL = "/card/421495195122/?root_category_id=elektronika_UhWUEm";
    private static final String ID = "421495195122";
    private static final String ROOT_CATEGORY_ID_VALUE = "elektronika_UhWUEm";

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
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Редактировать» для активного оффера, по ховеру - кнопка «Снять с продажи»")
    public void shouldSeeActiveOfferActionsHover() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setActiveOfferAvaliableActions())).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().hover();

        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(isDisplayed());
        basePageSteps.onMyOffersPage().popup().should(hasText("Снять с продажи"));
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

        basePageSteps.onMyOffersPage().snippetFirst().button(ACTIVATE).should(isDisplayed());
        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Активировать» для неактивного оффера, по ховеру - кнопки «Редактировать» и «Удалить»")
    public void shouldSeeInactiveOfferActionsHover() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                        .setInactiveWithReason(SOLD_ON_YANDEX))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().hover();

        basePageSteps.onMyOffersPage().snippetFirst().button(ACTIVATE).should(isDisplayed());
        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(isDisplayed());
        basePageSteps.onMyOffersPage().popup().should(hasText("Удалить"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Узнать причину» для забаненного оффера, без возможности редактирования")
    public void shouldSeeBannedOfferActions() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setCardLinkUrl(URL).setBannedNoEditOfferAvaliableActions()
                        .setBannedWithReason(STOPWORD))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().snippetFirst().link(KNOW_REASON).should(hasAttribute(HREF, urlSteps.testing()
                .path(CARD).path(ID).path(SLASH).queryParam(ROOT_CATEGORY_ID_PARAM, ROOT_CATEGORY_ID_VALUE).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Узнать причину» для забаненного оффера, без возможности редактирования," +
            " по ховеру - кнопка «Удалить»")
    public void shouldSeeBannedOfferActionsHover() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setCardLinkUrl(URL)
                        .setBannedNoEditOfferAvaliableActions().setBannedWithReason(STOPWORD))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().hover();

        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).should(not(isDisplayed()));
        basePageSteps.onMyOffersPage().snippetFirst().link(KNOW_REASON).should(hasAttribute(HREF, urlSteps.testing()
                .path(CARD).path(ID).path(SLASH).queryParam(ROOT_CATEGORY_ID_PARAM, ROOT_CATEGORY_ID_VALUE).toString()));
        basePageSteps.onMyOffersPage().popup().should(hasText("Удалить"));
    }

}
