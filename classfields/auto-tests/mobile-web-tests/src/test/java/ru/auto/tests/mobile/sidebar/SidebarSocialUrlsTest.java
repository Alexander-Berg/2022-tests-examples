package ru.auto.tests.mobile.sidebar;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар - кнопки соцсетей")
@Feature(SIDEBAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SidebarSocialUrlsTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка Вконтакте")
    @Owner(DSVICHIHIN)
    public void shouldClickVkButton() {
        basePageSteps.onMainPage().sidebar().vkButton()
                .should(hasAttribute("href", "https://vk.com/autoru_news")).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка Одноклассники")
    @Owner(DSVICHIHIN)
    public void shouldClickOdnoklassnikiButton() {
        basePageSteps.onMainPage().sidebar().odnoklassnikiButton()
                .should(hasAttribute("href", "https://ok.ru/group/52852027556005")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка Youtube")
    @Owner(DSVICHIHIN)
    public void shouldClickYoutubeButton() {
        basePageSteps.onMainPage().sidebar().youtubeButton()
                .should(hasAttribute("href", "https://www.youtube.com/user/AutoRuTv")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
