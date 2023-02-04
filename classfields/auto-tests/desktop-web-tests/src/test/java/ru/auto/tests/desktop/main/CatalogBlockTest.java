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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_NARROW_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - новинки каталога")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CatalogBlockTest {

    private static final int NEWS_CNT = 3;

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsShowcase").post();

        urlSteps.testing().open();
        basePageSteps.setWideWindowSize(HEIGHT_1024);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onMainPage().catalogNews().title().should(isDisplayed()).hover().click();
        urlSteps.testing().path(CATALOG).path(CARS).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение моделей")
    public void shouldSeeModels() {
        basePageSteps.onMainPage().catalogNews().modelsList().should(hasSize(NEWS_CNT))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onMainPage().catalogNews().getModel(0).hover().click();
        urlSteps.testing().path(CATALOG).path(CARS).path("/hyundai/creta/22913367/22913409/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Перейти в каталог»")
    public void shouldClickGoToCatalogUrl() {
        basePageSteps.onMainPage().catalogNews().goToCatalogUrl().should(isDisplayed()).hover().click();
        urlSteps.testing().path(CATALOG).path(CARS).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ресайзинг")
    public void shouldResizeCatalogBlock() {
        basePageSteps.setWindowSize(WIDTH_NARROW_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().catalogNews().getModel(1).should(not(isDisplayed()));
        basePageSteps.onMainPage().catalogNews().getModel(2).should(not(isDisplayed()));

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().catalogNews().getModel(1).should(isDisplayed());
        basePageSteps.onMainPage().catalogNews().getModel(2).should(isDisplayed());

    }
}
