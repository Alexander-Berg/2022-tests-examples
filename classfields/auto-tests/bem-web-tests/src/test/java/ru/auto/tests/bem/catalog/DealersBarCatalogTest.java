package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок дилеров")
@Feature(AutoruFeatures.DEALERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealersBarCatalogTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public String dealerUrl;

    @Parameterized.Parameter(2)
    public String dealersUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/catalog/",
                        "from=listing-mini-listing-list",
                        "%s/moskva/dilery/cars/new/?from=listing-mini-listing-list"},
                {"/catalog/cars/toyota/",
                        "from=listing-mini-listing-list",
                        "%s/moskva/dilery/cars/toyota/new/?from=listing-mini-listing-list"},
                {"/catalog/cars/toyota/corolla/",
                        "from=listing-mini-listing-list",
                        "%s/moskva/dilery/cars/toyota/new/?from=listing-mini-listing-list"},
                {"/catalog/cars/hyundai/solaris/20922677/",
                        "from=listing-mini-listing-list",
                        "%s/moskva/dilery/cars/hyundai/new/?from=listing-mini-listing-list"},
                {"/catalog/cars/hyundai/solaris/20922677/20922742/",
                        "from=listing-mini-listing-list",
                        "%s/moskva/dilery/cars/hyundai/new/?from=listing-mini-listing-list"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(url).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onBasePage().footer(), 0, -100);
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Кликаем ссылку с заголовка блока дилеров")
    public void shouldSeeDealersHeaderUrl() {
        shouldClickDealersUrl(basePageSteps.onBasePage().dealersBlock().headerUrl());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Кликаем кнопку «Все дилеры»")
    public void shouldSeeAllDealersUrl() {
        shouldClickDealersUrl(basePageSteps.onBasePage().dealersBlock().allDealersButton());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Кликаем ссылку на конкретного дилера")
    public void shouldClickDealerUrl() {
        basePageSteps.onBasePage().dealersBlock().getDealer(0).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(containsString(dealerUrl));
        basePageSteps.onDealerCardPage().info().waitUntil(isDisplayed());
    }

    @Step("Проверяем ссылку")
    private void shouldClickDealersUrl(VertisElement button) {
        button.waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(format(dealersUrl, urlSteps.getConfig().getTestingURI()))
                .ignoreParam("view_type").shouldNotSeeDiff();
    }
}
