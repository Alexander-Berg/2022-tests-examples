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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - помощник - кузова")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HelperBlockBodiesTest {

    private String salesCount;

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

    //@Parameter("Название кузова")
    @Parameterized.Parameter
    public String bodyName;

    //@Parameter("Запрос")
    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String query;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Седан", "/body-sedan/", ""},
                {"Внедорожник", "", "%1$s=ALLROAD_3_DOORS&%1$s=ALLROAD_5_DOORS"},
                {"Хэтчбек", "", "%1$s=HATCHBACK_3_DOORS&%1$s=HATCHBACK_5_DOORS&%1$s=LIFTBACK"},
                {"Универсал", "body-wagon/", ""},
                {"Купе", "/body-coupe/", ""},
                {"Лифтбек", "/body-liftback/", ""},
                {"Пикап", "/body-pickup/", ""},
                {"Минивэн", "/body-minivan/", ""},
                {"Кабриолет", "/body-cabrio/", ""},
                {"Фургон", "/body-van/", ""}
        });
    }

    @Before
    public void before() {
        //mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213").post();

        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().marksBlock().switcher("Помощник").click();
        salesCount = basePageSteps.onMainPage().marksBlock().resultsButton().getText();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кузову")
    public void shouldClickBody() {
        basePageSteps.onMainPage().marksBlock().body(bodyName).waitUntil(isDisplayed()).click();
        System.out.println(basePageSteps.onMainPage().marksBlock().resultsButton().getText());
        basePageSteps.onMainPage().marksBlock().resultsButton().waitUntil(not(hasText(salesCount)));
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(ALL).path(path).replaceQuery(format(query, "body_type_group"))
                .addParam("from", "old_guru").shouldNotSeeDiff();
    }
}