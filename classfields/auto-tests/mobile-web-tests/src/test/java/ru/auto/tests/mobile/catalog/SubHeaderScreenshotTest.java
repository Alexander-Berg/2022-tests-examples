package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - подшапка")
@Feature(AutoruFeatures.CATALOG)
public class SubHeaderScreenshotTest {

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
    public String text;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"", "ОБЪЯВЛЕНИЯКАТАЛОГГАРАЖВЫКУПВИДЕООТЗЫВЫ"},
                {"/vaz/", "ОБЪЯВЛЕНИЯКАТАЛОГГАРАЖВЫКУПВИДЕОСТАТИСТИКА ЦЕНОТЗЫВЫ"},
                {"/vaz/vesta/", "ОБЪЯВЛЕНИЯКАТАЛОГГАРАЖВЫКУПВИДЕОСТАТИСТИКА ЦЕНОТЗЫВЫ"},
                {"/vaz/vesta/20417749/", "ОБЪЯВЛЕНИЯКАТАЛОГГАРАЖВЫКУПВИДЕОСТАТИСТИКА ЦЕНОТЗЫВЫ"},
                {"/vaz/vesta/20417749/20417777/", "ОБЪЯВЛЕНИЯКАТАЛОГГАРАЖВЫКУПВИДЕОСТАТИСТИКА ЦЕНОТЗЫВЫ"},
                {"/vaz/vesta/20417749/20417777/specifications/20417777_20726112_20417814/",
                        "ОБЪЯВЛЕНИЯКАТАЛОГГАРАЖВЫКУПВИДЕОСТАТИСТИКА ЦЕНОТЗЫВЫ"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(url).open();
    }

    @Test
    @DisplayName("Отображение вкладок")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeTabs() {
        basePageSteps.onCatalogPage().subHeader().should(hasText(text));
    }
}
