package ru.auto.tests.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SHIVA;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Проверка Shiva")
@Feature(SHIVA)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class ShivaTest {

    private static final String URL_TEMPLATE = "https://%s/release";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Проверка, что в тестинге и бранче production разные пакеты")
    public void shouldSeeDifferentPackages() {
        urlSteps.fromUri(format(URL_TEMPLATE, urlSteps.getConfig().getTestingDomain())).open();

        String testingPackage = basePageSteps.onBasePage().body().should(hasText(startsWith("af-desktop="))).getText();

        cookieSteps.setCookie(urlSteps.getConfig().getBranchCookieName(), "production",
                format(".%s", urlSteps.getConfig().getTestingDomain()));
        urlSteps.refresh();

        String prodPackage = basePageSteps.onBasePage().body().should(hasText(startsWith("af-desktop="))).getText();

        assertThat("В тестинге и бранче production одинаковые пакеты", testingPackage, not(equalTo(prodPackage)));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Проверка, что в тестинге и в бранче autotest в проде одинаковые пакеты")
    public void shouldSeeSamePackages() {
        urlSteps.fromUri(format(URL_TEMPLATE, urlSteps.getConfig().getTestingDomain())).open();

        String testingPackage = basePageSteps.onBasePage().body().should(hasText(startsWith("af-desktop="))).getText();

        urlSteps.fromUri(format(URL_TEMPLATE, urlSteps.getConfig().getAutoruProdDomain())).open();
        cookieSteps.setCookie(urlSteps.getConfig().getBranchCookieName(), "autotest",
                format(".%s", urlSteps.getConfig().getAutoruProdDomain()));
        urlSteps.refresh();

        String autotestPackage = basePageSteps.onBasePage().body().should(hasText(startsWith("af-desktop="))).getText();

        assertThat("В тестинге и бранче autotest разные пакеты", testingPackage, equalTo(autotestPackage));
    }
}
