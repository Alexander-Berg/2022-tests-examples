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

@Epic(MY_OFFERS_FEATURE)
@Feature("Состояния сниппета")
@DisplayName("Снипет снятый с продажи с причиной")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetInactiveWithReasonTest {

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

    @Parameterized.Parameter(2)
    public String messageText;

    @Parameterized.Parameter(3)
    public String badgeText;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Неактивный оффер по причине «Не удалось дозвониться» для продавца",
                        mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions().setCantCallInactiveStatus(),
                        CANT_CALL_REASON_TITLE, REMOVED},
                {"Товар снятый с продажи по причине «Закончился срок размещения»",
                        mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                                .setInactiveWithReason(EXPIRED),
                        EXPIRED.getName(), ENDED}
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
    @DisplayName("Снипет снятый с продажи с причиной")
    public void shouldSeeInactiveSnippetStatusAndBadge() {
        basePageSteps.onMyOffersPage().snippetFirst().statusText().should(hasText(messageText));
        basePageSteps.onMyOffersPage().snippetFirst().badge(RED).should(hasText(badgeText));
    }

}
