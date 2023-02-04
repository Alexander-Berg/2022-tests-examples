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
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_SHARE_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цель «CARD_OFFER_SHARE_CLICK» при клике по шарингу на карточке")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardShareGoalTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter(0)
    public String shareService;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"vkontakte"},
                {"telegram"},
                {"facebook"},
                {"twitter"},
                {"odnoklassniki"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().snippetFirst().hover().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_SHARE_CLICK)
    @DisplayName("Цель «CARD_OFFER_SHARE_CLICK» при клике по шарингу на карточке")
    public void shouldSeeCardOfferShareClickGoal() {
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.scrollingToElement(basePageSteps.onOfferCardPage().footer());
        basePageSteps.onOfferCardPage().shareButton().click();
        basePageSteps.onOfferCardPage().shareService(shareService).click();

        goalsSteps.withGoalType(CARD_OFFER_SHARE_CLICK)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setServiceId(shareService))
                .withCount(1)
                .shouldExist();
    }

}
