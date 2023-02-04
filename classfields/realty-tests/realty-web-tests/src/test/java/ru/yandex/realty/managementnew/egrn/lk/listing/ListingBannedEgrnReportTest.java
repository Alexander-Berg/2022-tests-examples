package ru.yandex.realty.managementnew.egrn.lk.listing;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EGRN_REPORTS;

@Issue("VERTISTEST-1522")
@DisplayName("Отчет ЕГРН. Листинг отчетов")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ListingBannedEgrnReportTest {

    private static final String MESSAGE = "Вы не можете размещать и редактировать объявления, так как ваш аккаунт " +
            "заблокирован за нарушения условий. Соглашения об информационном сотрудничестве";

    private static final String LOGIN = "ztestOwner";
    private static final String PASSWORD = "Mytests2021!";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.login(LOGIN, PASSWORD);
        urlSteps.testing().path(MANAGEMENT_NEW_EGRN_REPORTS).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Забаненный пользователь")
    public void shouldSeeBannedEgrnReportOnListing() {
        managementSteps.onManagementNewPage().notification(MESSAGE).should(isDisplayed());
        managementSteps.onManagementNewPage().lkParanja().should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Забаненный пользователь. Скриншот")
    public void shouldSeeBannedEgrnReportScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(managementSteps.onEgrnListingPage().pageBody());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(managementSteps.onEgrnListingPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
