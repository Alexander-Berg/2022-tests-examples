package ru.auto.tests.amp.catalog;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
@Feature(AutoruFeatures.AMP)
@DisplayName("Каталог - главная - новинки")
public class MainNoveltiesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение новинок")
    public void shouldSeeNovelties() {
        basePageSteps.onCatalogMainPage().novelties().waitUntil(isDisplayed());
        basePageSteps.onCatalogMainPage().novelties().loader().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Клик по новинке")
    @Owner(DSVICHIHIN)
    public void shouldClickNovelty() {
        basePageSteps.onCatalogMainPage().novelties().click();
        basePageSteps.onCatalogBodyPage().description().waitUntil(isDisplayed());
    }
}