package ru.yandex.general.goals;

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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.CardStatus.ACTIVE;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.VAS_TOP_BUY;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.MyOfferSnippet.RAISE_UP;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на ВАС'ы")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class VasGoalsTest {

    private static final String ID = "12345";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(VAS_TOP_BUY)
    @DisplayName("Цель «VAS_TOP_BUY» при тапе на «Поднять» с карточки")
    public void shouldSeeCardOfferVasTopBuyFromCard() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(true).setId(ID).setStatus(ACTIVE)
                        .setVas().build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        basePageSteps.onOfferCardPage().button(RAISE_UP).click();

        goalsSteps.withGoalType(VAS_TOP_BUY)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setPage("Card"))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(VAS_TOP_BUY)
    @DisplayName("Цель «VAS_TOP_BUY» при тапе на «Поднять» с ЛК сниппета")
    public void shouldSeeProfileListingVasTopBuyFromLkSnippet() {
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).setId(ID).setActiveOfferAvaliableActions()
                                .setVas())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(OFFERS).open();
        basePageSteps.onMyOffersPage().snippetFirst().button(RAISE_UP).click();

        goalsSteps.withGoalType(VAS_TOP_BUY)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setPage("MyOffers"))
                .withCount(1)
                .shouldExist();
    }

}
