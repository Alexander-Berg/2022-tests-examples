package ru.auto.tests.forms.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;

//import io.qameta.allure.Parameter;

@DisplayName("Шапка - ссылки")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderUrlsTest {

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

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Легковые", "https://%s/moskva/cars/all/"},
                {"Коммерческие", "https://%s/moskva/lcv/all/"},
                {"Мото", "https://%s/moskva/motorcycle/all/"},
                {"ПроАвто", "https://%s/history/?from=shapka"},
                {"Кредиты", "https://%s/promo/finance/"},
                {"ОСАГО", "https://%s/promo/osago/"},
                {"Дилерам", "https://%s/dealer/"},
                {"Журнал", "https://mag.%s/?utm_campaign=main_menu&utm_content=main_menu&utm_medium=cpm&utm_source=auto-ru"},
        });
    }

    @Before
    public void before() {
        basePageSteps.setWideWindowSize(WIDTH_WIDE_PAGE);
        urlSteps.testing().path(MOTO).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке в шапке")
    public void shouldClickHeaderUrl() {
        basePageSteps.onFormsPage().header().line2().button(title).click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getTestingURI().getHost())).shouldNotSeeDiff();
    }
}
