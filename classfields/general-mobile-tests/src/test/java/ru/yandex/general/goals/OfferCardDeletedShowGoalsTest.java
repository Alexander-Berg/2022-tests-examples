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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.CardStatus.REMOVED;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_DELETED_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «CARD_OFFER_DELETED_SHOW», при показе карточки удаленного оффера продавцу/покупателю")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardDeletedShowGoalsTest {

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
    public boolean isOwner;

    @Parameterized.Parameters(name = "{index}. Открываем удаленный оффер «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"за продавца", true},
                {"за покупателя", false}
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
    @Feature(CARD_OFFER_DELETED_SHOW)
    @DisplayName("Цель «CARD_OFFER_DELETED_SHOW», при показе карточки удаленного оффера продавцу/покупателю")
    public void shouldSeeCardOfferDeletedShowGoalOnOpenCard() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard(BASIC_CARD).setIsOwner(isOwner).setId(ID)
                        .setStatus(REMOVED).setDeletedOfferAvaliableActions().build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(CARD_OFFER_DELETED_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
