package ru.yandex.realty.managementnew.profsearch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.element.profsearch.ProfOffer;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.MANAGEMENT_NEW;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.page.ProfSearchPage.EXPORT;
import static ru.yandex.realty.page.ProfSearchPage.MOSCOW_RGID;
import static ru.yandex.realty.page.ProfSearchPage.RGID;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.RetrofitApiSteps.reportDate;

@DisplayName("Выгрузка офферов из ЛК")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class GetExcelReportTest {

    private Path filePath;
    private List<String> expectedOffers = new ArrayList<>();
    private List<String> offersFromReport;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    public TemporaryFolder files = new TemporaryFolder();


    @Inject
    @ProfsearchAccount
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private PassportSteps passportSteps;


    @Before
    public void openPage() {
        filePath = files.getRoot().toPath().resolve("report.xls");

        passportSteps.login(account);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, MOSCOW_RGID).open();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Все офферы")
    public void shouldSaveAllLink() {
        basePageSteps.onProfSearchPage().checkAll().click();
        expectedOffers = basePageSteps.onProfSearchPage().offerLinks();
        basePageSteps.onProfSearchPage().button(EXPORT).click();

        offersFromReport = retrofitApiSteps
                .downloadReport(filePath, basePageSteps.session(), reportDate())
                .offersFromReport();

        shouldSeeOffersInReport();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Только первый оффер")
    public void shouldSaveFirstLink() {
        basePageSteps.onProfSearchPage().offer(FIRST).showExtra().clickIf(isDisplayed());
        addLink(basePageSteps.onProfSearchPage().offer(FIRST));

        basePageSteps.onProfSearchPage().button(EXPORT).click();

        offersFromReport = retrofitApiSteps
                .downloadReport(filePath, basePageSteps.session(), reportDate())
                .offersFromReport();

        shouldSeeOffersInReport();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Два оффера")
    public void shouldSaveCustomLinks() {
        basePageSteps.onProfSearchPage().offerList().stream().limit(2)
                .forEach(offer -> offer.showExtra().clickIf(isDisplayed()));
        addLink(basePageSteps.onProfSearchPage().offer(FIRST));
        addLink(basePageSteps.onProfSearchPage().offer(FIRST + 1));

        basePageSteps.onProfSearchPage().button(EXPORT).click();

        offersFromReport = retrofitApiSteps
                .downloadReport(filePath, basePageSteps.session(), reportDate())
                .offersFromReport();

        shouldSeeOffersInReport();
    }

    private void addLink(ProfOffer offer) {
        offer.offerCheckbox().clickWhile(isChecked());
        expectedOffers.add(offer.convertToProdLink());
    }

    @Step("Список офферов в файле должен быть полным")
    private void shouldSeeOffersInReport() {
        assertThat("В репорте должны быть нужные офферы", offersFromReport, equalTo(expectedOffers));
    }
}
