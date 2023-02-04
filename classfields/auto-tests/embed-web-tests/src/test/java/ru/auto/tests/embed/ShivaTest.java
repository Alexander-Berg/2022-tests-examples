package ru.auto.tests.embed;

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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SHIVA;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.EMBED;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_PROMO;
import static ru.auto.tests.desktop.consts.Pages.VIN;

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

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Проверка, что в тестинге и в бранче production разные пакеты")
    public void shouldSeeDifferentPackages() {
        urlSteps.subdomain(SUBDOMAIN_PROMO).path(EMBED).path(VIN)
                .addParam("_debug_embed", "true")
                .addParam("theme", "autoru").open();

        String testingPackage = browserMockSteps.getAutoruAppIdForUrl(urlSteps.toString());

        cookieSteps.setCookieForBaseDomain(urlSteps.getConfig().getBranchCookieName(), "production");
        urlSteps.refresh();

        String prodPackage = browserMockSteps.getAutoruAppIdForUrl(urlSteps.toString());

        assertThat(testingPackage, startsWith("af-embed="));
        assertThat(prodPackage, startsWith("af-embed="));
        assertThat("В тестинге и в бранче production одинаковые пакеты", testingPackage, not(equalTo(prodPackage)));
    }
}
