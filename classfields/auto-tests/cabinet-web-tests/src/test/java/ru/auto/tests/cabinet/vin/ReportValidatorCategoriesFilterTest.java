package ru.auto.tests.cabinet.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.File;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.VALIDATOR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фильтры в валидаторе отчётов")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ReportValidatorCategoriesFilterTest {

    private static final String REPORTS_ZIP_FILE_PATH = new File("src/main/resources/reports.zip").getAbsolutePath();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/ApiV1AdminValidateFileBatchThreeFiles").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(HISTORY).path(VALIDATOR).open();

        basePageSteps.onCabinetReportValidatorPage().inputFile().sendKeys(REPORTS_ZIP_FILE_PATH);
        basePageSteps.onCabinetReportValidatorPage().statusDescription().waitUntil(isDisplayed())
                .waitUntil(hasText("1 файл\nЗагружено"));
        basePageSteps.onCabinetReportValidatorPage().button("Проверить").click();
        basePageSteps.onCabinetReportValidatorPage().validationResult().results().waitUntil(hasSize(3));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация только «Сервис»")
    public void shouldFilterOnlyService() {
        basePageSteps.onCabinetReportValidatorPage().validationFilters().button("Сервис").click();

        basePageSteps.onCabinetReportValidatorPage().validationResult().results().should(hasSize(2));
        basePageSteps.onCabinetReportValidatorPage().validationResult().should(hasText("Ошибки · 10" +
                "\nПредупреждения · 2\nСервис\nreport.json\nНе валидное значение VIN\n1\nСервис\n" +
                "report.xml\nНе валидное значение VIN\n1\nСтатистика\nВсе файлы\nВсего записей 2\n" +
                "С ошибками 10\nС предупреждением 2\nНевалидных VIN 0\nУникальных VIN 2\nС пробегом 2" +
                "\nС нулевым пробегом 0\nСоблюдается уникальность ID ДА\nЗаписей с работами 2"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация только «Доп. оборудование»")
    public void shouldFilterOnlyAdditionalEquipment() {
        basePageSteps.onCabinetReportValidatorPage().validationFilters().button("Доп. оборудование").click();

        basePageSteps.onCabinetReportValidatorPage().validationResult().results().should(hasSize(1));
        basePageSteps.onCabinetReportValidatorPage().validationResult().should(hasText("Ошибки · 5\n" +
                "Предупреждения · 1\nДоп. оборудование\nreport.csv\nНе валидное значение VIN\n1\n" +
                "Статистика\nВсе файлы\nВсего записей 1\nС ошибками 5\nС предупреждением 1\nНевалидных " +
                "VIN 0\nУникальных VIN 1\nС пробегом 1\nС нулевым пробегом 0\nСоблюдается уникальность ID " +
                "ДА\nЗаписей с работами 1"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация и «Сервис», и «Доп. оборудование»")
    public void shouldFilterAll() {
        basePageSteps.onCabinetReportValidatorPage().validationFilters().button("Доп. оборудование").click();
        basePageSteps.onCabinetReportValidatorPage().validationResult().results().should(hasSize(1));
        basePageSteps.onCabinetReportValidatorPage().validationFilters().button("Сервис").click();

        basePageSteps.onCabinetReportValidatorPage().validationResult().results().should(hasSize(3));
        basePageSteps.onCabinetReportValidatorPage().validationResult().should(hasText("Ошибки · 15\n" +
                "Предупреждения · 3\nСервис\nreport.json\nНе валидное значение VIN\n1\nСервис\nreport.xml" +
                "\nНе валидное значение VIN\n1\nДоп. оборудование\nreport.csv\nНе валидное значение VIN" +
                "\n1\nСтатистика\nВсе файлы\nВсего записей 3\nС ошибками 15\nС предупреждением 3\n" +
                "Невалидных VIN 0\nУникальных VIN 3\nС пробегом 3\nС нулевым пробегом 0\nСоблюдается " +
                "уникальность ID ДА\nЗаписей с работами 3"));
    }
}
