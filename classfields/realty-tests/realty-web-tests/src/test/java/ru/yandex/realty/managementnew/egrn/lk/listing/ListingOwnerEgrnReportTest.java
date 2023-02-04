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
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EGRN_REPORTS;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.page.BasePage.REPORTS;

@Issue("VERTISTEST-1522")
@DisplayName("Отчет ЕГРН. Листинг отчетов")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ListingOwnerEgrnReportTest {

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
        apiSteps.createVos2Account(account, AccountType.OWNER);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Есть «Отчеты» в попапе юзера")
    public void shouldSeeUserPopupReportLine() {
        urlSteps.testing().open();
        managementSteps.moveCursor(managementSteps.onManagementNewPage().headerMain().userAccount());
        managementSteps.onManagementNewPage().userNewPopup().link(REPORTS)
                .should(hasHref(containsString(MANAGEMENT_NEW_EGRN_REPORTS)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Есть «Отчеты» в подхедере ЛК")
    public void shouldSeeHearUnderReportLine() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().tab(REPORTS).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Пустой листинг отчетов")
    public void shouldSeeEmptyEgrnListing() {
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS).open();
        managementSteps.onEgrnListingPage().watchFlats()
                .should(hasHref(equalTo(urlSteps.testing().path("/proverka-kvartiry/").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ошибка листинга отчетов")
    public void shouldSeeErrorEgrnListing() {
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS)
                .queryParam("disable-api", "egrn-paid-report.getPaidReport").open();
        managementSteps.onEgrnListingPage().egrnErrorPage().waitUntil(isDisplayed());
        managementSteps.onEgrnListingPage().egrnErrorPage().spanLink("Обновить страницу").should(isDisplayed());
    }
}
