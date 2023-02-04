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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннер «Это объявление популярнее, чем ваше»")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MorePopularBannerTest {

    private static final String SALE_ID = "/1090110718-50e4924a/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionMorePopular",
                "desktop/AdTargetUserMorePopular",
                "desktop/OfferCarsMorePopular",
                "desktop/OfferCarsStatsMorePopular").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Закрытие баннера")
    public void shouldCloseBanner() {
        basePageSteps.onCardPage().morePopularBanner().closeButton().click();
        basePageSteps.onCardPage().morePopularBanner().waitUntil(not(isDisplayed()));
        basePageSteps.refresh();
        basePageSteps.onCardPage().morePopularBanner().should(not(isDisplayed()));
        cookieSteps.shouldSeeCookie("vas-motivator-closed");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Клик по кнопке «Выбрать опцию для продвижения»")
    public void shouldClickChooseOptionButton() {
        basePageSteps.onCardPage().morePopularBanner().button("Выбрать опцию для продвижения").click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(CARS).shouldNotSeeDiff();
    }
}