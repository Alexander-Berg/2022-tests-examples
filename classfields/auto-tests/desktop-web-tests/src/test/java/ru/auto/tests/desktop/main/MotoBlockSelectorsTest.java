package ru.auto.tests.desktop.main;

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
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - блок «Мототехника» - селекторы")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MotoBlockSelectorsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Селект")
    @Parameterized.Parameter
    public String selectName;

    //@Parameter("Опция в селекте")
    @Parameterized.Parameter(1)
    public String selectItem;

    //@Parameter("Параметр")
    @Parameterized.Parameter(2)
    public String param;

    //@Parameter("Запрос")
    @Parameterized.Parameter(3)
    public String query;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Тип мотоцикла", "Все внедорожные ", "moto_type", "%1$s=OFF_ROAD_GROUP&%1$s=ALLROUND&%1$s=OFFROAD_ENDURO&%1$s=CROSS_COUNTRY&%1$s=SPORTENDURO&%1$s=TOURIST_ENDURO"},
                {"Тип мотоцикла", "\u00a0\u00a0Allround ", "moto_type", "%1$s=ALLROUND"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор опции в селекте")
    public void shouldSelectItem() {
        basePageSteps.onMainPage().motoBlock().selectItem(selectName, selectItem);
        basePageSteps.onMainPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onMainPage().motoBlock().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.path(MOTORCYCLE).path(ALL).replaceQuery(format(query, param)).shouldNotSeeDiff();
    }
}
