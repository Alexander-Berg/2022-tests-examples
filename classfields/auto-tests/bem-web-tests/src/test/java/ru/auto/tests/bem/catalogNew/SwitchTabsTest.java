package ru.auto.tests.bem.catalogNew;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CATALOG_NEW;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.page.CatalogNewPage.GENERATION_TAB;
import static ru.auto.tests.desktop.page.CatalogNewPage.MARK;
import static ru.auto.tests.desktop.page.CatalogNewPage.MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка модели - табы")
@Epic(CATALOG_NEW)
@Feature("Табы")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SwitchTabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

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
                {"Кузов", "Кабриолет", "autoru_body_type", "cabrio"},
                {"Двигатель", "Бензин", "engine_type", "gasoline"},
                {"Год от", "2018", "year_from", "2018"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Issue("AUTORUFRONT-22267")
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение фильтров при переключении табов")
    public void shouldSeeSaveFilterWithSwitchingTab() {
        catalogPageSteps.onCatalogNewPage().filter().button(filter).waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogNewPage().activePopup().waitUntil(isDisplayed());
        catalogPageSteps.onCatalogNewPage().activeListItemByContains(filterValue).click();
        catalogPageSteps.onCatalogNewPage().filter().buttonContains("Показать").click();

        catalogPageSteps.onCatalogNewPage().navigationTab(GENERATION_TAB).waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().cardGenerations().waitUntil(isDisplayed());

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SLASH)
                .addParam(urlParameter, urlParameterValue).shouldNotSeeDiff();
    }
}
