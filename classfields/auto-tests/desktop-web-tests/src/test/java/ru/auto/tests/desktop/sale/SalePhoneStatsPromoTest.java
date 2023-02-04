package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.CALLS_HISTORY_TOOLTIP;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо статистики звонков")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SalePhoneStatsPromoTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String COOKIE = "calls-promote-closed";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/OfferCarsUsedUserOwner"),
                stub("desktop/UserOffersCarsCallHistory")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID)
                .addParam(FORCE_POPUP, CALLS_HISTORY_TOOLTIP).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение промо")
    public void shouldSeePromo() {
        basePageSteps.onCardPage().callHistoryPromo().waitUntil(isDisplayed())
                .should(hasText("Здесь вы можете отслеживать звонки и жаловаться на нежелательные.\nХорошо, спасибо."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Закрытие промо по клику на кнопку «Хорошо, спасибо»")
    public void shouldClosePromoByUrlClick() {
        basePageSteps.onCardPage().callHistoryPromo().okButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().callHistoryPromo().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(COOKIE, "true");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Закрытие промо по клику за его пределами")
    public void shouldClosePromoByOutsideClick() {
        basePageSteps.onCardPage().cardHeader().title().should(isDisplayed()).click();
        basePageSteps.onCardPage().callHistoryPromo().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(COOKIE, "true");
    }
}
