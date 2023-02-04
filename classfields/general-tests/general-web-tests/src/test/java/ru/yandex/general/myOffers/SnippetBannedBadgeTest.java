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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.CardStatus.CANT_CALL_REASON_TITLE;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.EXPIRED;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.STOPWORD;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_PHOTO;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.Badge.BLOCKED;
import static ru.yandex.general.element.Badge.ENDED;
import static ru.yandex.general.element.Badge.RED;
import static ru.yandex.general.element.Badge.REMOVED;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature("Состояния сниппета")
@DisplayName("Бейдж для забаненного сниппета")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetBannedBadgeTest {

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCabinetSnippet mockCabinetSnippet;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Товар забаненный",
                        mockSnippet(BASIC_SNIPPET).setBannedOfferAvaliableActions()
                                .setBannedWithReason(WRONG_OFFER_CATEGORY)},
                {"Резюме 2 бана нередактируемый оффер",
                        mockSnippet(BASIC_SNIPPET).setBannedNoEditOfferAvaliableActions()
                                .setBannedWithReason(STOPWORD, SPAM)},
                {"Товар 3 бана редактируемый оффер",
                        mockSnippet(BASIC_SNIPPET).setBannedOfferAvaliableActions()
                                .setBannedWithReason(STOPWORD, WRONG_PHOTO, WRONG_OFFER_CATEGORY)}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setCabinetListing(cabinetListingResponse().offers(asList(
                mockCabinetSnippet)).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Бейдж для забаненного сниппета, текст статуса не отображается")
    public void shouldSeeBannedSnippetBadge() {
        basePageSteps.onMyOffersPage().snippetFirst().badge(RED).should(hasText(BLOCKED));
        basePageSteps.onMyOffersPage().snippetFirst().statusText().should(not(isDisplayed()));
    }

}
