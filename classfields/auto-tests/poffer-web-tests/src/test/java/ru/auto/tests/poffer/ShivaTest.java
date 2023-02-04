package ru.auto.tests.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SHIVA;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Проверка Shiva")
@Feature(SHIVA)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class ShivaTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private SeleniumMockSteps browserMockSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Проверка, что в тестинге и проде разные пакеты")
    public void shouldSeeDifferentPackages() {
        String testingPackage = browserMockSteps.getAutoruAppIdForUrl(urlSteps.toString());

        cookieSteps.setCookieForBaseDomain(urlSteps.getConfig().getBranchCookieName(), "production");
        urlSteps.refresh();

        String prodPackage = browserMockSteps.getAutoruAppIdForUrl(urlSteps.toString());

        assertThat(testingPackage, startsWith("af-poffer="));
        assertThat(prodPackage, startsWith("af-poffer="));
        assertThat("В тестинге и в бранче production одинаковые пакеты", testingPackage, not(equalTo(prodPackage)));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Проверка, что в тестинге и в бранче autotest в проде одинаковые пакеты")
    public void shouldSeeSamePackages() {
        String testingPackage = browserMockSteps.getAutoruAppIdForUrl(urlSteps.toString());

        urlSteps.autoruProdURI().path(CARS).path(USED).path(ADD).open();
        cookieSteps.setCookie(urlSteps.getConfig().getBranchCookieName(), "autotest",
                format(".%s", urlSteps.getConfig().getAutoruProdDomain()));
        urlSteps.refresh();

        String autotestPackage = browserMockSteps.getAutoruAppIdForUrl(urlSteps.toString());

        assertThat("В тестинге и в бранче autotest в проде разные пакеты", testingPackage, equalTo(autotestPackage));
    }
}
