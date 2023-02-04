package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - главная")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).open();
        screenshotSteps.setWindowSizeForScreenshot();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки в списке популярных марок")
    public void shouldSelectPopularMark() throws IOException {
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Марка, модель, поколение ")
                .should(isDisplayed()).click();
        String mark = catalogPageSteps.selectFirstMark();
        urlSteps.path(CARS).path(mark.toLowerCase()).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки в списке всех марок")
    public void shouldSelectUnpopularMark() throws IOException {
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Марка, модель, поколение ")
                .should(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().switcher("Все").waitUntil(isDisplayed())
                .click();
        String mark = catalogPageSteps.selectFirstMark();
        urlSteps.path(CARS).path(mark.toLowerCase()).path("/").ignoreParam("view_type").shouldNotSeeDiff();
    }
}