package ru.yandex.realty.managementnew.egrn.lk.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.page.BasePage.REPORTS;

@Issue("VERTISTEST-1522")
@Tag(JURICS)
@DisplayName("Отчет ЕГРН. Листинг отчетов")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ListingJuricEgrnReportTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Отчеты» в попапе юзера")
    public void shouldSeeUserPopupReportLine() {
        urlSteps.testing().open();
        managementSteps.moveCursor(managementSteps.onManagementNewPage().headerMain().userAccount());
        managementSteps.onManagementNewPage().userNewPopup().link(REPORTS).should(exists());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «Отчеты» в подхедере ЛК")
    public void shouldSeeHearUnderReportLine() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().tab(REPORTS).should(exists());
    }
}
