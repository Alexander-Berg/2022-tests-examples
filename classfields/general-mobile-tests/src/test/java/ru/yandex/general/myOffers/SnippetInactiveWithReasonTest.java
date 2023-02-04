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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TEXT;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TITLE;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.EXPIRED;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.STOPWORD;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.Badge.BLOCKED;
import static ru.yandex.general.element.Badge.ENDED;
import static ru.yandex.general.element.Badge.REMOVED;
import static ru.yandex.general.mobile.element.MyOfferSnippet.KNOW_REASON;
import static ru.yandex.general.mobile.element.MyOfferSnippet.KNOW_REASONS;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(MY_OFFERS_FEATURE)
@Feature("Состояния сниппета")
@DisplayName("Снипет снятый с продажи с причиной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SnippetInactiveWithReasonTest {

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

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет снятый с продажи с причиной «Вы долго не выходили на связь»")
    public void shouldSeeInactiveCantCallSnippet() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions().setCantCallInactiveStatus())).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().badge().should(hasText(REMOVED));
        basePageSteps.onMyOffersPage().snippetFirst().toastText().should(hasText(REMOVED));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет снятый с продажи с причиной «Вы долго не выходили на связь», причина в попапе")
    public void shouldSeeInactiveCantCallSnippetPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions().setCantCallInactiveStatus())).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().spanLink(KNOW_REASON).click();

        basePageSteps.onMyOffersPage().popup().titleList().should(hasSize(1)).get(0).should(hasText(CANT_CALL_REASON_TITLE));
        basePageSteps.onMyOffersPage().popup().textList().should(hasSize(1)).get(0).should(hasText(CANT_CALL_REASON_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет забаненный с причиной «Неверная категория»")
    public void shouldSeeBannedSnippet() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setBannedOfferAvaliableActions()
                        .setBannedWithReason(WRONG_OFFER_CATEGORY))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().toastText().should(hasText(BLOCKED));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет забаненный с причиной «Неверная категория», причина в попапе")
    public void shouldSeeBannedSnippetPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setBannedOfferAvaliableActions()
                        .setBannedWithReason(WRONG_OFFER_CATEGORY))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().spanLink(KNOW_REASON).click();

        basePageSteps.onMyOffersPage().popup().titleList().should(hasSize(1)).get(0).should(
                hasText(WRONG_OFFER_CATEGORY.getTitle()));
        basePageSteps.onMyOffersPage().popup().textList().should(hasSize(1)).get(0).should(
                hasText(WRONG_OFFER_CATEGORY.getReasonNoLinks()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет забаненный с двумя причинами и нередактируемый")
    public void shouldSeeBannedTwoReasonsNoEditSnippet() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setBannedNoEditOfferAvaliableActions()
                        .setBannedWithReason(STOPWORD, SPAM))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().toastText().should(hasText(BLOCKED));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет забаненный с двумя причинами и нередактируемый, причина в попапе")
    public void shouldSeeBannedTwoReasonsNoEditSnippetPopup() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setBannedNoEditOfferAvaliableActions()
                        .setBannedWithReason(STOPWORD, SPAM))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onMyOffersPage().snippetFirst().spanLink(KNOW_REASONS).click();

        basePageSteps.onMyOffersPage().popup().h3().should(hasText("2 причины"));
        basePageSteps.onMyOffersPage().popup().titleList().should(hasSize(2)).should(
                hasItems(hasText(STOPWORD.getTitle()),
                        hasText(SPAM.getTitle())));
        basePageSteps.onMyOffersPage().popup().textList().should(hasSize(2)).should(
                hasItems(hasText(STOPWORD.getReasonNoLinks()),
                        hasText(SPAM.getReasonNoLinks())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сниппет с истекшим сроком размещения")
    public void shouldSeeExpiredSnippet() {
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                        .setInactiveWithReason(EXPIRED))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onMyOffersPage().snippetFirst().badge().should(hasText(ENDED));
    }

}
