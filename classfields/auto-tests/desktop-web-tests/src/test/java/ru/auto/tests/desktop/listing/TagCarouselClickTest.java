package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - карусель тегов")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TagCarouselClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String startPath;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String rangeName;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL, "/moskva/cars/all/do-100000/", "до  100\u00a0000\u00a0₽"},
                {NEW, "/moskva/cars/new/tag/new4new/", "Новинки автопрома"},
                {USED, "/moskva/cars/used/do-250000/", "до  250\u00a0000\u00a0₽"},
                {"/hyundai/used/", "/moskva/cars/hyundai/used/do-300000/", "до  300\u00a0000\u00a0₽"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(startPath).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по тегу")
    public void shouldClickTag() {
        basePageSteps.onListingPage().tagCarousel().tag(rangeName).waitUntil(isDisplayed()).click();
        urlSteps.fromUri(format("%s%s", urlSteps.getConfig().getTestingURI(), path)).ignoreParam("sort")
                .shouldNotSeeDiff();
    }
}
