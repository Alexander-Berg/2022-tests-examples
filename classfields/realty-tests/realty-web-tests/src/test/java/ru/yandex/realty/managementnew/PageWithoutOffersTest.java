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
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Частные лица. Отображение страницы без объявлений»")
@Issue("VERTISTEST-818")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PageWithoutOffersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementNewPage;

    @Inject
    private CompareSteps compareSteps;


    @Before
    public void before() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Должны видеть личный кабинет без офферов")
    public void shouldSeeRefillPopup() {
        Screenshot testing = compareSteps.getElementScreenshot(managementNewPage.onManagementNewPage().offersBlock());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshot(managementNewPage.onManagementNewPage().offersBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
