package ru.auto.tests.desktop.main;

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
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_NARROW_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Канал Авто.ру")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AutoruChannelTest {

    private static final String CHANNEL_URL = "https://www.youtube.com/user/AutoRuTv/featured";
    private static final int VIDEOS_COUNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока")
    public void shouldSeeBlock() {
        basePageSteps.onMainPage().autoruChannel().videosList().should(hasSize(VIDEOS_COUNT))
                .forEach(i -> i.should(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onMainPage().autoruChannel().title().should(hasAttribute("href", CHANNEL_URL)).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по видео")
    public void shouldClickVideo() {
        basePageSteps.onMainPage().autoruChannel().getVideo(0).hover().click();
        basePageSteps.onMainPage().videoPopup().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все видео»")
    public void shouldClickAllVideosUrl() {
        basePageSteps.onMainPage().autoruChannel().allVideosUrl().should(hasAttribute("href", CHANNEL_URL))
                .click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ресайзинг")
    public void shouldResize() {
        basePageSteps.setWindowSize(WIDTH_NARROW_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().autoruChannel().getVideo(3).should(not(isDisplayed()));

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().autoruChannel().getVideo(3).waitUntil(isDisplayed());
    }
}