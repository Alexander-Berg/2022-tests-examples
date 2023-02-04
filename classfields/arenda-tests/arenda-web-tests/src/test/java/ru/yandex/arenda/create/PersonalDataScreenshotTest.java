package ru.yandex.arenda.create;

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
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_ISSUE_DATE_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_SERIES_AND_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.PATRONYMIC_ID;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Скриншоты")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PersonalDataScreenshotTest {

    private static final String HEADER_TEXT_PERSONAL_DATA = "Личные данные";

    private static final String PASSPORT_TEXT = "9999999999";
    private static final String ISSUE_DATE_TEXT = "01012000";
    private static final String PATRONOMYC = "Валерыч";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        compareSteps.resizeDesktop();
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
    }

    @Test
    @DisplayName("Скриншот страницы с незаполненными данными")
    public void shouldAnketaScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onBasePage().root());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onBasePage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Скриншот страницы с заполненными данными")
    public void shouldSeeDataScreenshot() {
        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).sendKeys(PASSPORT_TEXT);
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).sendKeys(ISSUE_DATE_TEXT);
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).sendKeys(PATRONOMYC);
        lkSteps.onLkPage().headerText(HEADER_TEXT_PERSONAL_DATA).click();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onBasePage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).sendKeys(PASSPORT_TEXT);
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).sendKeys(ISSUE_DATE_TEXT);
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).sendKeys(PATRONOMYC);
        lkSteps.onLkPage().headerText(HEADER_TEXT_PERSONAL_DATA).click();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onBasePage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
