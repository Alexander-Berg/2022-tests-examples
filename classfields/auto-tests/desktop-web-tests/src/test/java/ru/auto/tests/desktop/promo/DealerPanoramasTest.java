package ru.auto.tests.desktop.promo;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.PANORAMAS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - дилеры - панорамы")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerPanoramasTest {

    private long pageOffset;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(DEALER).path(PANORAMAS).open();
        pageOffset = basePageSteps.getPageYOffset();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по якорной ссылке в сайдбаре")
    public void shouldClickAnchorUrl() {
        basePageSteps.onPromoDealerPanoramasPage().sidebar().button("Как снимать панорамы").click();
        urlSteps.shouldNotSeeDiff();
        waitSomething(3, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по обычной ссылке в сайдбаре")
    public void shouldClickUrl() {
        basePageSteps.onPromoDealerPanoramasPage().sidebar().button("Размещение и продвижение").click();
        urlSteps.testing().path(DEALER).shouldNotSeeDiff();
        basePageSteps.onPromoDealerPanoramasPage().form().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onPromoDealerPanoramasPage().h1().should(hasText("3D-съёмка машин на Авто.ру"));
        basePageSteps.onPromoDealerPanoramasPage().sidebar().should(isDisplayed());
        basePageSteps.onPromoDealerPanoramasPage().panoramasHowToVideo().should(isDisplayed());
        basePageSteps.onPromoDealerPanoramasPage().noticeableImage().should(isDisplayed());
        basePageSteps.onPromoDealerPanoramasPage().pointsImage().should(isDisplayed());
        basePageSteps.onPromoDealerPanoramasPage().interiorImage().should(isDisplayed());
    }
}