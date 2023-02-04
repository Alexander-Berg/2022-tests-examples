package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.page.CatalogNewPage.MARK;
import static ru.auto.tests.desktop.page.CatalogNewPage.MODEL;
import static ru.auto.tests.desktop.page.CatalogNewPage.SPECIFICATION_TAB;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка модели - табы")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SwitchTabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String filter;

    @Parameterized.Parameter(1)
    public String filterValue;

    @Parameterized.Parameter(2)
    public String urlParameter;

    @Parameterized.Parameter(3)
    public String urlParameterValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Тип кузова", "Кабриолет", "body_type_group", "cabrio"},
                {"Двигатель", "Бензин", "engine_group", "gasoline"},
                {"Год от", "2018", "year_from", "2018"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение фильтров при переключении табов")
    public void shouldSeeSaveFilterWithSwitchingTab() {
        basePageSteps.onCatalogPage().filter().button(filter).waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().activeListItemByContains(filterValue).click();
        basePageSteps.onCatalogPage().filter().buttonContains("Показать").click();

        basePageSteps.onCatalogPage().button(SPECIFICATION_TAB).waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogNewPage().specificationContentBlock().waitUntil(isDisplayed());

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SPECIFICATIONS)
                .path(SLASH).addParam(urlParameter, urlParameterValue.toUpperCase()).shouldNotSeeDiff();
    }
}
