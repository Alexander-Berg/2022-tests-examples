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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Удаление модели из сравнения в каталоге")
@Feature(AutoruFeatures.COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DeleteModelFromCompareTest {

    private static final String MODEL_URL = "/vaz/xray/20497289/20497306/";
    private static final int COMPARE_COUNT = 0;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    private CatalogPageSteps catalogSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MODEL_URL).open();
        catalogSteps.addToCompare();
        catalogSteps.deleteFromCompare();
    }

    @Test
    @DisplayName("Удаление из сравнения на карточке модели")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldDeleteFromCompareFromModelCard() {
        urlSteps.testing().path(COMPARE_MODELS).open();
        steps.onComparePage().modelsList().should(hasSize(COMPARE_COUNT));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение плашки")
    public void shouldSeeNotify() {
        catalogSteps.onCatalogPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено"));
    }
}