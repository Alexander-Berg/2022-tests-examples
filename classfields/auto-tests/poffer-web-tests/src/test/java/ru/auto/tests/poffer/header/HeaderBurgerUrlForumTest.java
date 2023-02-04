package ru.auto.tests.poffer.header;

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

import static ru.auto.tests.desktop.TestData.CLIENT_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_FORUM;
import static ru.auto.tests.desktop.consts.Pages.USED;

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
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Форумы» в мега-меню авторизованным")
    public void shouldClickForumsInMegaMenuAuthorized() {
        loginSteps.loginAs(CLIENT_PROVIDER.get());
        urlSteps.testing().path(CARS).path(USED).path(ADD).open();

        basePageSteps.onPofferPage().header().burgerButton().hover();
        basePageSteps.onPofferPage().header().burger().button("Форумы").click();

        urlSteps.subdomain(SUBDOMAIN_FORUM).shouldNotSeeDiff();
    }

}
