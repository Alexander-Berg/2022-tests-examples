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
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_SOLD_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «CARD_OFFER_SOLD_SHOW», при показе карточки проданного оффера для продавца/покупателя")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardSoldShowGoalsTest {

    private static final String ID = "123456";

    private MockResponse mockResponse;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCard mockCard;

    @Parameterized.Parameters(name = "{index}. Открываем оффер снятый по причине «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {SOLD_ON_YANDEX.getName(), mockCard(BASIC_CARD).setInactiveReason(SOLD_ON_YANDEX.getMockValue())},
                {SOLD_SOMEWHERE.getName(), mockCard(BASIC_CARD).setInactiveReason(SOLD_SOMEWHERE.getMockValue())}
        });
    }

    @Before
    public void before() {
        mockResponse = mockResponse().setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_SOLD_SHOW)
    @DisplayName("Цель «CARD_OFFER_SOLD_SHOW», при показе карточки проданного оффера для продавца")
    public void shouldSeeCardOfferSoldShowGoalOnOpenCardOwner() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setIsOwner(true).setId(ID)
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions().build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(CARD_OFFER_SOLD_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_SOLD_SHOW)
    @DisplayName("Цель «CARD_OFFER_SOLD_SHOW», при показе карточки проданного оффера для покупателя")
    public void shouldSeeCardOfferSoldShowGoalOnOpenCardBuyer() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setIsOwner(false).setId(ID)
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions().build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(CARD_OFFER_SOLD_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
