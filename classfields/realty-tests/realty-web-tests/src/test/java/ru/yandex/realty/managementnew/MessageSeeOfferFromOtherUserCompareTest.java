package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Частные лица. Наличие системных сообщений")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MessageSeeOfferFromOtherUserCompareTest {

    private static final String LOGIN = "management-reader";
    private static final String PASSW = "Qwerty123";
    private static final String UID = "4002615357";
    private static final String NOTIFICATION = "Вы просматриваете личный кабинет от лица";

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

    @Inject
    private ApiSteps apiSteps;

    @Before
    public void before() {
        account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        apiSteps.createVos2Account(account, AGENT);
        passportSteps.login(account);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("bizdev", "1")
                .queryParam("vos_user_login", "4001053214").open();
    }

    @Description("НУЖЕН ДРУГОЙ ПОЛЬЗОВАТЕЛЬ")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Должны видеть кабинет другого юзера")
    public void shouldSeeOtherUser() {
        Screenshot testing = compareSteps.getElementScreenshot(basePageSteps.onManagementNewPage()
                .notification(NOTIFICATION));

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshot(
                basePageSteps.onManagementNewPage().notification(NOTIFICATION));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
