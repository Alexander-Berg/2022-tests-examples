package ru.auto.tests.desktop.promo;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CONFERENCES;
import static ru.auto.tests.desktop.consts.Pages.DEALER;

@DisplayName("Промо - дилеры - сайдбар - ссылки")
@Feature(PROMO)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealerSidebarUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    //@Parameter("Название ссылки")
    @Parameterized.Parameter(1)
    public String title;

    //@Parameter("Ссылка")
    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"", "Конференции Авто.ру", "https://%s/dealer/conferences/"},
                {"", "3D панорамы машин", "https://%s/dealer/panoramas/"},
                {"", "Медийная реклама", "https://%s/dealer/display/"},
                {"", "О проекте", "https://%s/home/"},
                {"", "Логотип Авто.ру", "https://%s/design/"},

                {CONFERENCES, "Размещение и продвижение", "https://%s/dealer/"},
                {CONFERENCES, "3D панорамы машин", "https://%s/dealer/panoramas/"},
                {CONFERENCES, "Медийная реклама", "https://%s/dealer/display/"},
                {CONFERENCES, "О проекте", "https://%s/home/"},
                {CONFERENCES, "Логотип Авто.ру", "https://%s/design/"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(DEALER).path(startUrl).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылки")
    public void shouldClickUrl() {
        basePageSteps.onPromoDealerPage().sidebar().button(title).click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}