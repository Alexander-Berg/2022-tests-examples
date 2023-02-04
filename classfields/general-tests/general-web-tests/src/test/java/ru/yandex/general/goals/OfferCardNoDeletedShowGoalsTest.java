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
import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.EXPIRED;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.OTHER;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.RETHINK;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_SOMEWHERE;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_DELETED_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(GOALS_FEATURE)
@DisplayName("Нет цели «CARD_OFFER_DELETED_SHOW», при показе карточки не удаленного оффера для покупателя/продавца")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardNoDeletedShowGoalsTest {

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

    @Parameterized.Parameters(name = "{index}. Открываем оффер в статусе «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {SOLD_ON_YANDEX.getName(), mockCard(BASIC_CARD).setInactiveReason(SOLD_ON_YANDEX.getMockValue())
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions()},
                {SOLD_SOMEWHERE.getName(), mockCard(BASIC_CARD).setInactiveReason(SOLD_SOMEWHERE.getMockValue())
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions()},
                {EXPIRED.getName(), mockCard(BASIC_CARD).setInactiveReason(EXPIRED.getMockValue())
                        .setStatus(INACTIVE).setExpiredOfferAvaliableActions()},
                {RETHINK.getName(), mockCard(BASIC_CARD).setInactiveReason(RETHINK.getMockValue())
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions()},
                {OTHER.getName(), mockCard(BASIC_CARD).setInactiveReason(OTHER.getMockValue())
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions()},
                {"Вы долго не выходили на связь", mockCard(BASIC_CARD).setCantCallInactiveReason()
                        .setStatus(INACTIVE).setInactiveOfferAvaliableActions()},
                {"Активаная карточка", mockCard(BASIC_CARD)},
                {"Забаненная карточка", mockCard(BASIC_CARD).setStatus(BANNED).setBanReasons(SPAM)}
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
    @DisplayName("Нет цели «CARD_OFFER_DELETED_SHOW», при показе карточки не удаленного оффера для продавца")
    public void shouldSeeNoCardOfferDeletedShowGoalOnOpenCardOwner() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setIsOwner(true).setId(ID).build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(CARD_OFFER_DELETED_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_DELETED_SHOW)
    @DisplayName("Нет цели «CARD_OFFER_DELETED_SHOW», при показе карточки не удаленного оффера для покупателя")
    public void shouldSeeNoCardOfferDeletedShowGoalOnOpenCardBuyer() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setIsOwner(false).setId(ID).build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(CARD_OFFER_DELETED_SHOW)
                .withCount(0)
                .shouldExist();
    }

}
