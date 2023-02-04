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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.Badge.BLUE;
import static ru.yandex.general.element.Badge.ENDED;
import static ru.yandex.general.element.Badge.RED;
import static ru.yandex.general.element.Badge.SOLD;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(MY_OFFERS_FEATURE)
@Feature("Состояния сниппета")
@DisplayName("Снипет снятый с продажи пользователем")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetInactiveTest {

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
    public String badgeText;

    @Parameterized.Parameter(3)
    public String badgeColor;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Товар снятый с продажи по причине «Продал на Яндексе»",
                        mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                                .setInactiveWithReason(SOLD_ON_YANDEX), SOLD, BLUE},
                {"Товар снятый с продажи по причине «Продал в другом месте»",
                        mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                                .setInactiveWithReason(SOLD_SOMEWHERE), SOLD, BLUE},
                {"Товар снятый с продажи по причине «Передумал продавать»",
                        mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                                .setInactiveWithReason(RETHINK), ENDED, RED},
                {"Товар снятый с продажи по причине «Другая причина»",
                        mockSnippet(BASIC_SNIPPET).setInactiveOfferAvaliableActions()
                                .setInactiveWithReason(OTHER), ENDED, RED}
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
    @DisplayName("Снипет снятый с продажи пользователем")
    public void shouldSeeBannedSnippetStatusAndBadge() {
        basePageSteps.onMyOffersPage().snippetFirst().badge().should(hasText(badgeText));
    }

}
