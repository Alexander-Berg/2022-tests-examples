package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.endsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление модели в сравнение в каталоге")
@Feature(AutoruFeatures.COMPARE)
@Story("Добавление")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddModelToCompareSpecificationsTest {

    private static final String MODEL_URL = "/land_rover/discovery/2307388/2307389/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    private CatalogPageSteps catalogSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String tabTitle;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Характеристики"},
                {"Комплектации"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MODEL_URL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление в сравнение в характеристиках/комплектации")
    public void shouldAddToCompareFromSpecifications() {
        catalogSteps.onCatalogPage().cardTab(tabTitle).waitUntil(isDisplayed()).click();
        catalogSteps.onCatalogPage().compareAddButtonSpecifications().waitUntil(isDisplayed()).click();
        catalogSteps.onCatalogPage().compareDeleteButtonSpecifications().waitUntil(isDisplayed());
        urlSteps.testing().path(COMPARE_MODELS).open();
        steps.onComparePage().getModel(0).url().should(hasAttribute("href", endsWith(MODEL_URL)));
    }
}