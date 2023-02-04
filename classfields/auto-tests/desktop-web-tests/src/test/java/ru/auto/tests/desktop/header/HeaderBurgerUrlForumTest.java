package ru.auto.tests.desktop.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;
import static ru.auto.tests.desktop.TestData.USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_FORUM;
import static ru.auto.tests.desktop.consts.QueryParams.R;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;

@DisplayName("Кликаем по ссылке «Форумы» в мега-меню")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderBurgerUrlForumTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LoginSteps loginSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Форумы» в мега-меню авторизованным")
    public void shouldClickForumsInMegaMenuAuthorized() throws IOException {
        loginSteps.loginAs(USER_2_PROVIDER.get());
        openMainAndClickForums();

        urlSteps.subdomain(SUBDOMAIN_FORUM).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Форумы» в мега-меню без авторизации")
    public void shouldClickForumsInMegaMenuNotAuthorized() {
        openMainAndClickForums();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam(R, encode(format("https://%s.%s/", SUBDOMAIN_FORUM, urlSteps.getConfig().getBaseDomain())))
                .shouldNotSeeDiff();
    }

    private void openMainAndClickForums() {
        urlSteps.testing().path(MOSKVA).open();

        basePageSteps.onMainPage().header().burgerButton().hover();
        basePageSteps.onMainPage().header().burger().button("Форумы").click();
    }

}
