package ru.auto.tests.poffer.header;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;


@DisplayName("Шапка - ссылки")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderSecondLineTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Легковые", "https://%s/moskva/cars/all/"},
                {"Коммерческие", "https://%s/moskva/lcv/all/"},
                {"Мото", "https://%s/moskva/motorcycle/all/"},
                {"ПроАвто", "https://%s/history/?from=shapka"},
                {"Кредиты", "https://%s/promo" + FINANCE},
                {"ОСАГО", "https://%s/promo/osago/"},
                {"Дилерам", "https://%s/dealer/"},
                {"Журнал", "https://mag.%s/?utm_campaign=main_menu&utm_content=main_menu&utm_medium=cpm&utm_source=auto-ru"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("poffer/UserDraftCarsEmpty")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в шапке")
    public void shouldClickHeaderUrl() {
        basePageSteps.onPofferPage().header().line2().button(title).click();

        urlSteps.fromUri(format(url, urlSteps.getConfig().getTestingURI().getHost())).shouldNotSeeDiff();
    }
}
