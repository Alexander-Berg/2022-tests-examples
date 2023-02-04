package ru.yandex.arenda.admin.contract;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.arenda.constants.UriPath.CONTRACT;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.pages.ContractPage.ADD_CONTRACT_BUTTON;

@Link("https://st.yandex-team.ru/VERTISTEST-1719")
@DisplayName("[Админка] Тесты на страницу договора")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class ContractNewPageScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        compareSteps.resize(1600, 5000);
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);

        String createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        passportSteps.adminLogin();
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(CONTRACT).open();
    }

    @Test
    @DisplayName("Видим скрин страницы создания договора с ошибками ")
    public void shouldSeeNewContractPageScreenshot() {
        lkSteps.onContractPage().button(ADD_CONTRACT_BUTTON).click();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onContractPage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onContractPage().button(ADD_CONTRACT_BUTTON).click();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onContractPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
