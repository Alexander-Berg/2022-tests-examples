package ru.auto.tests.desktop.crosslinks;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.QueryParams.SORT;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - блок перелинковки")
@Feature(LISTING)
@Story("Блок перелинковки «Все о Toyota/Toyota Corolla»")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CrossLinksAboutBlockUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String urlTitle;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/moskva/cars/toyota/all/", "Каталог Toyota", "/catalog/cars/toyota/"},
                {"/moskva/cars/toyota/all/", "Toyota хэтчбек 5 дв.", "/moskva/cars/toyota/all/body-hatchback_5_doors/"},

                {"/moskva/cars/toyota/corolla/all/", "Каталог Toyota Corolla", "/catalog/cars/toyota/corolla/"},
                {"/moskva/cars/toyota/corolla/all/", "Toyota Corolla хэтчбек 5 дв.", "/moskva/cars/toyota/corolla/all/body-hatchback_5_doors/"},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(startUrl).open();
        basePageSteps.onListingPage().crossLinksBlock().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onListingPage().crossLinksBlock().buttonContains("Показать все").waitUntil(isDisplayed())
                .hover().click();
        basePageSteps.onListingPage().crossLinksBlock().button(urlTitle).waitUntil(isDisplayed())
                .hover().click();
        urlSteps.testing().path(url).ignoreParam(SORT).shouldNotSeeDiff();
    }
}
