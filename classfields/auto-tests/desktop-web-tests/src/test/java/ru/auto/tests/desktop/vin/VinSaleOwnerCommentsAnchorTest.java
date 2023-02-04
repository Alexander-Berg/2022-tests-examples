package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Комментарии в отчёте - переход к комментарию из превью")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinSaleOwnerCommentsAnchorTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String VIN_PROMO_POPUP_COOKIE = "promo_popup_history_seller_closed";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserOwner",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        cookieSteps.setCookieForBaseDomain(VIN_PROMO_POPUP_COOKIE, "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход к комментарию из превью")
    public void shouldMoveToComment() {
        long pageOffset = basePageSteps.getPageYOffset();
        basePageSteps.onCardPage().vinReport().button("Комментировать").click();
        waitSomething(1, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
        urlSteps.onCurrentUrl().fragment("block-pts_owners").shouldNotSeeDiff();
        basePageSteps.onCardPage().vinReport().commentInput().should(isDisplayed());
        basePageSteps.onCardPage().vinReport().button("Комментировать").waitUntil(not(isDisplayed()));
    }
}