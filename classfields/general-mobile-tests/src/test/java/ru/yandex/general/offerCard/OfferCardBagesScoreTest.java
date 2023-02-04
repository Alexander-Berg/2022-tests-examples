package ru.yandex.general.offerCard;

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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(OFFER_CARD_FEATURE)
@Feature("Юзер-скор")
@DisplayName("Разное кол-во полосок активности в юзер-скоре")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardBagesScoreTest {

    private static final String ID = "12345";

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String userBadgeScore;

    @Parameterized.Parameter(2)
    public int activeIndicatorsCount;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Нет рейтинга", "None", 0},
                {"Бейдж 1/3", "Ten", 1},
                {"Бейдж 2/3", "Twenty", 2},
                {"Бейдж 3/3", "Thirty", 3}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setId(ID).setUserBadgeScore(userBadgeScore).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Разное кол-во полосок активности в юзер-скоре")
    public void shouldSeeActiveScoreIndicatorCount() {
        basePageSteps.onOfferCardPage().sellerInfo().activeScoreIndicatorList().should(hasSize(activeIndicatorsCount));
    }

}
