package ru.yandex.realty.managementnew.egrn.lk.listing;

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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EGRN_REPORTS;

@Issue("VERTISTEST-1522")
@DisplayName("Отчет ЕГРН. Листинг отчетов")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ListingSuccessEgrnReportTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AccountType.OWNER);
        mockRuleConfigurable.paidReportStub(account.getId(), "mock/egrn/paidReportsSuccess.json").createWithDefaults();
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Успешное формирование. нет сообщений")
    public void shouldSeeSuccessEgrnReportOnListing() {
        managementSteps.onEgrnListingPage().firstEgrnSnippet().message()
                .should(not(exists()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Успешное формирование. Скриншот")
    public void shouldSeeSuccessEgrnReportScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(managementSteps.onEgrnListingPage().firstEgrnSnippet());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(managementSteps.onEgrnListingPage().firstEgrnSnippet());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
