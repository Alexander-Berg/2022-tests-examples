package ru.yandex.general.goals;

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
import ru.yandex.general.beans.GoalRequestBody;
import ru.yandex.general.consts.CardStatus;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.CardStatus.ACTIVE;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_OWNER_INACTIVATE;
import static ru.yandex.general.consts.Goals.PROFILE_LISTING_OFFERS_OFFER_INACTIVATE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.OfferCardPage.DEACTIVATE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цель при снятии оффера")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferInactivateGoalTest {

    private static final String ID = "12345";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

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
    public CardStatus.CardDeactivateStatuses deactivateStatus;

    @Parameterized.Parameters(name = "{index}. Цель с причиной снятия оффера «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {SOLD_ON_YANDEX.getName(), SOLD_ON_YANDEX},
                {SOLD_SOMEWHERE.getName(), SOLD_SOMEWHERE},
                {RETHINK.getName(), RETHINK},
                {OTHER.getName(), OTHER}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_OWNER_INACTIVATE)
    @DisplayName("Цель «CARD_OFFER_OWNER_INACTIVATE», при снятии оффера с карточки")
    public void shouldSeeOfferInactivateGoalCard() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setStatus(ACTIVE).setActiveOfferAvaliableActions()
                        .setIsOwner(true).setId(ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().more().waitUntil(isDisplayed()).click();
        basePageSteps.onOfferCardPage().popup().spanLink(DEACTIVATE).click();
        basePageSteps.onOfferCardPage().recallOfferPopup().deactivateOfferWithReason(deactivateStatus);

        GoalRequestBody requestBody = goalRequestBody()
                .setOfferId(ID)
                .setInactiveRecallReason(deactivateStatus.getGoalValue());


        goalsSteps.withGoalType(CARD_OFFER_OWNER_INACTIVATE)
                .withCurrentPageRef()
                .withBody(requestBody)
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PROFILE_LISTING_OFFERS_OFFER_INACTIVATE)
    @DisplayName("Цель «PROFILE_LISTING_OFFERS_OFFER_INACTIVATE», при снятии оффера в ЛК")
    public void shouldSeeOfferInactivateGoalLK() {
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).setId(ID).setActiveOfferAvaliableActions())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(OFFERS).open();

        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();
        basePageSteps.onMyOffersPage().recallOfferPopup().deactivateOfferWithReason(deactivateStatus);
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);


        GoalRequestBody requestBody = goalRequestBody()
                .setOfferId(ID)
                .setInactiveRecallReason(deactivateStatus.getGoalValue());

        goalsSteps.withGoalType(PROFILE_LISTING_OFFERS_OFFER_INACTIVATE)
                .withCurrentPageRef()
                .withBody(requestBody)
                .withCount(1)
                .shouldExist();
    }

}
