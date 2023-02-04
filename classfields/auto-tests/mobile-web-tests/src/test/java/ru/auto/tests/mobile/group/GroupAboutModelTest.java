package ru.auto.tests.mobile.group;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ABOUT;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка - о модели")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GroupAboutModelTest {

    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String PATH = "/21342050-21342121/";

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
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsConfigurationsGallery").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(PATH).path(ABOUT).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие полноэкранной галереи")
    public void shouldOpenFullscreenGallery() {
        basePageSteps.onGroupAboutModelPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().waitUntil(isDisplayed());
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().counter().waitUntil(hasText("1 / 45"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие правильного изображения")
    public void shouldSeeCorrectImage() {
        String image = basePageSteps.onGroupAboutModelPage().gallery().getItem(1).image().getAttribute("src");
        basePageSteps.onGroupAboutModelPage().gallery().getItem(1).waitUntil(isDisplayed()).click();
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().counter().waitUntil(hasText("2 / 45"));
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().getItem(1).image()
                .waitUntil(hasAttribute("src", image));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие полноэкранной галереи")
    public void shouldCloseFullscreenGallery() {
        basePageSteps.onGroupAboutModelPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().closeIcon().waitUntil(isDisplayed()).hover().click();
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переключение цветов")
    public void shouldSwitchColors() {
        basePageSteps.onGroupAboutModelPage().getColor(1).click();
        basePageSteps.onGroupAboutModelPage().gallery().getItem(10).click();
        basePageSteps.onGroupAboutModelPage().fullScreenGallery().counter().waitUntil(hasText("11 / 45"));
    }
}
