package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;

@DisplayName("Частные лица. Страница заблокированного юзера»")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BannedUserCompareTest {

    private static final String LOGIN = "marbya43";
    private static final String PASSW = "vfif19";
    private static final String UID = "4003805569";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        passportSteps.login(account);
        compareSteps.resize(1920, 2500);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Заходим под ззаблокированным юзером, сравниваем скриншоты")
    public void shouldSeeBannedUser() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().root());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onManagementNewPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
