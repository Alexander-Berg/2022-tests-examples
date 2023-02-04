package ru.auto.tests.mobile.pager;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Каталог - пагинация на главной")
@Feature(PAGER)
public class PagerCatalogTest {

    private static final int MODELS_PER_PAGE = 34;
    private static final int NEEDED_SCROLL = 35000;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).open();
        basePageSteps.onCatalogPage().modelsList().should(hasSize(MODELS_PER_PAGE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка следующей страницы")
    @Category({Regression.class})
    public void shouldSeeNextPage() {
        basePageSteps.scrollDown(NEEDED_SCROLL);
        basePageSteps.onCatalogPage().modelsList().waitUntil(hasSize(MODELS_PER_PAGE * 2));
        urlSteps.addParam("page_num", "2").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка предыдущей страницы")
    @Category({Regression.class})
    public void shouldSeePreviousPage() {
        urlSteps.onCurrentUrl().addParam("page_num", "2").open();
        basePageSteps.onCatalogPage().modelsList().waitUntil(hasSize(MODELS_PER_PAGE));
        basePageSteps.onCatalogPage().prevButton().should(isDisplayed()).click();
        basePageSteps.onCatalogPage().modelsList().waitUntil(hasSize(MODELS_PER_PAGE * 2));
        basePageSteps.scrollUp(NEEDED_SCROLL);
        urlSteps.replaceParam("page_num", "1").shouldNotSeeDiff();
    }
}
