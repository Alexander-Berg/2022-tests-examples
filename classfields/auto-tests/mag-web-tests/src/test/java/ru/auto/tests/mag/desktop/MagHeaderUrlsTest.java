package ru.auto.tests.mag.desktop;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;

//import io.qameta.allure.Parameter;

@DisplayName("Шапка в журнале - ссылки")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MagHeaderUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Название ссылки")
    @Parameterized.Parameter
    public String title;

    //@Parameter("Ссылка")
    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Легковые", "https://%s/moskva/cars/all/"},
                {"Мото", "https://%s/moskva/motorcycle/all/"},
                {"Коммерческие", "https://%s/moskva/lcv/all/"},
                {"ПроАвто", "https://%s/history/?from=shapka"},
                {"Кредиты", "https://%s/promo" + FINANCE},
                {"ОСАГО", "https://%s/promo/osago/"},
                {"Журнал", "https://mag.%s/?utm_campaign=main_menu&utm_content=main_menu&utm_medium=cpm&utm_source=auto-ru"},
                {"Дилерам", "https://%s/dealer/"}
        });
    }

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_MAG).open();
        basePageSteps.setWideWindowSize(768);
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.refresh();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в шапке")
    public void shouldClickHeaderUrl() {
        String expectedUrl = format(url, urlSteps.getConfig().getTestingURI().getHost());
        basePageSteps.onMagPage().header().line2().button(title).click();
        urlSteps.fromUri(expectedUrl).ignoreParam("geo_id").ignoreParam("rgid").ignoreParam("no-redir")
                .shouldNotSeeDiff();
    }
}
