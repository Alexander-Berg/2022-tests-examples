package ru.auto.tests.forms.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import retrofit2.Response;
import ru.auto.test.passport.model.CreateUserResult;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;

@DisplayName("Частник, мотоциклы - подменный номер")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MotorcyclesPhonesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportApiAdaptor adaptor;

    @Inject
    private LoginSteps loginSteps;

    @Before
    public void before() throws IOException {
        Account account = createAccountWithMoscowPhone();
        loginSteps.loginAs(account);
        formsSteps.createMotorcyclesForm();

        urlSteps.testing().path(MOTO).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Объявление с подменным номером")
    public void shouldAddSaleWithRedirectPhone() throws IOException, InterruptedException {
        formsSteps.fillForm(formsSteps.getPhoto().getBlock());

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/forms/saveDraftFormsToPublicApi",
                hasJsonBody("drafts/user_moto_redirect_phones.json")
        ));
    }

    @Step("Создаём аккаунт с московским номером")
    private Account createAccountWithMoscowPhone() {
        String phone = getRandomPhone().replaceFirst("^7000", "7985");
        Response<CreateUserResult> userResp = adaptor.createAccountWithoutConfirmationByPhone(phone, "autoru");
        adaptor.confirmPhone(userResp.body().getConfirmationCode(), phone);
        return Account.builder().id((userResp.body()).getUser().getId())
                .login(phone.replace("7000", "7985")).password("autoru").phone(Optional.of(phone))
                .build();
    }
}
