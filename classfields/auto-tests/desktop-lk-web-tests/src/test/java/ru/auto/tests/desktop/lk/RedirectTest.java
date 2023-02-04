package ru.auto.tests.desktop.lk;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;

@DisplayName("Редиректы")
@Feature(AutoruFeatures.LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RedirectTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редирект в кабинет под дилером в объявлениях")
    public void shouldRedirectToCabinetByDealerInSales() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/UserDealer"),
                stub("desktop/SearchCarsBreadcrumbsRid213")
        ).create();

        urlSteps.testing().path(MY).open();

        urlSteps.subdomain(SUBDOMAIN_CABINET).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редирект в кабинет под агенством в объявлениях")
    public void shouldRedirectToCabinetByDealerAgencyInSales() {
        mockRule.setStubs(
                stub("desktop/SessionAuthAgency"),
                stub("desktop/UserDealerAgency"),
                stub("desktop/SearchCarsBreadcrumbsRid213")
        ).create();

        urlSteps.testing().path(MY).open();

        urlSteps.subdomain(SUBDOMAIN_CABINET).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Редирект в кабинет под дилером  в кошельке")
    public void shouldRedirectToCabinetByDealerInWallet() {
        mockRule.setStubs(stub("desktop/SessionAuthDealer")).create();

        urlSteps.testing().path(MY).path(WALLET).open();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Редирект на авторизацию под незарегом в кошельке")
    public void shouldRedirectToAuthByUnregInWallet() {
        urlSteps.testing().path(MY).path(WALLET).open();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s/my/wallet/", urlSteps.getConfig().getTestingURI())))
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Редирект на авторизацию под незарегом в настройках")
    public void shouldRedirectToAuthByUnregInSettings() {
        urlSteps.testing().path(MY).path(PROFILE).open();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s/my/profile/", urlSteps.getConfig().getTestingURI())))
                .shouldNotSeeDiff();
    }
}