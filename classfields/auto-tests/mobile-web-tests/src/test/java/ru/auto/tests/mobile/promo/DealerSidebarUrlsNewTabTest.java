package ru.auto.tests.mobile.promo;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CONFERENCES;
import static ru.auto.tests.desktop.consts.Pages.DEALER;

@DisplayName("Промо - дилеры - сайдбар - ссылки, открывающиеся в новой вкладке")
@Feature(PROMO)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealerSidebarUrlsNewTabTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

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
                {"", "Договор →", "https://yandex.ru/legal/autoru_cars_dogovor/"},

                {"", "Аналитика в Журнале →", "https://mag.test.avto.ru/theme/pro/"},

                {CONFERENCES, "Договор →", "https://yandex.ru/legal/autoru_cars_dogovor/"},
                {CONFERENCES, "Аналитика в Журнале →", "https://mag.test.avto.ru/theme/pro/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(DEALER).path(startUrl).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onPromoDealerPage().promoSidebar().button(title).click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}
