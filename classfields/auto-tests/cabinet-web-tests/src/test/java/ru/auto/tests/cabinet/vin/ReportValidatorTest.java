package ru.auto.tests.cabinet.vin;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.File;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.VALIDATOR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Валидатор отчётов")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ReportValidatorTest {

    private static final String TEXT = "[{\"id\":\"ТК50001976\",\"et\":2,\"v\":\"1HGED3555ML019116\"," +
            "\"dt\":\"2015-08-03\",\"er\":\"Белгород\",\"ec\":\"Белгород\",\"m\":250000,\"y\":1,\"clnttp\":1," +
            "\"amnt\":0,\"type\":\"regulation\",\"det\":\"ГТО\"}]";
    private static final String URL = "https://pastebin.com/raw/6yaK6suW";
    private static final String REPORT_JSON_FILE_PATH = new File("src/main/resources/report.json").getAbsolutePath();
    private static final String REPORT_XML_FILE_PATH = new File("src/main/resources/report.xml").getAbsolutePath();
    private static final String REPORT_CSV_FILE_PATH = new File("src/main/resources/report.csv").getAbsolutePath();
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
                "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(HISTORY).path(VALIDATOR).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Валидация файла через кнопку «Загрузить файл»")
    public void shouldValidateFile() {
        mockRule.with("cabinet/ApiV1AdminValidateFileBatch").update();

        basePageSteps.onCabinetReportValidatorPage().inputFile().sendKeys(REPORT_JSON_FILE_PATH);
        basePageSteps.onCabinetReportValidatorPage().status("Загружено").waitUntil(isDisplayed());
        basePageSteps.onCabinetReportValidatorPage().button("Проверить").click();
        basePageSteps.onCabinetReportValidatorPage().validationResult().errors()
                .waitUntil(hasText("Не валидное значение VIN\n1"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Валидация нескольких файлов разного формата через кнопку «Загрузить файл»")
    public void shouldValidateMultipleFiles() {
        mockRule.with("cabinet/ApiV1AdminValidateFileBatchThreeFiles").update();

        basePageSteps.onCabinetReportValidatorPage().inputFile().sendKeys(REPORT_JSON_FILE_PATH);
        basePageSteps.onCabinetReportValidatorPage().statusDescription().waitUntil(isDisplayed())
                .waitUntil(hasText("1 файл\nЗагружено"));

        basePageSteps.onCabinetReportValidatorPage().inputFile().sendKeys(REPORT_XML_FILE_PATH);
        basePageSteps.onCabinetReportValidatorPage().statusDescription().waitUntil(isDisplayed())
                .waitUntil(hasText("2 файла\nЗагружено"));

        basePageSteps.onCabinetReportValidatorPage().inputFile().sendKeys(REPORT_CSV_FILE_PATH);
        basePageSteps.onCabinetReportValidatorPage().statusDescription().waitUntil(isDisplayed())
                .waitUntil(hasText("3 файла\nЗагружено"));

        basePageSteps.onCabinetReportValidatorPage().button("Проверить").click();

        basePageSteps.onCabinetReportValidatorPage().validationResult().results().should(hasSize(3));
        basePageSteps.onCabinetReportValidatorPage().validationResult().should(hasText("Ошибки · 15" +
                "\nПредупреждения · 3\nСервис\nreport.json\nНе валидное значение VIN\n1\nСервис\n" +
                "report.xml\nНе валидное значение VIN\n1\nДоп. оборудование\nreport.csv\nНе валидное " +
                "значение VIN\n1\nСтатистика\nВсе файлы\nВсего записей 3\nС ошибками 15\nС предупреждением " +
                "3\nНевалидных VIN 0\nУникальных VIN 3\nС пробегом 3\nС нулевым пробегом 0\nСоблюдается " +
                "уникальность ID ДА\nЗаписей с работами 3"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Валидация ZIP файла с несколькими внутри через кнопку «Загрузить файл»")
    public void shouldValidateZipFile() {
        mockRule.with("cabinet/ApiV1AdminValidateFileBatchThreeFiles").update();

        basePageSteps.onCabinetReportValidatorPage().inputFile().sendKeys(REPORTS_ZIP_FILE_PATH);

        basePageSteps.onCabinetReportValidatorPage().statusDescription().waitUntil(isDisplayed())
                .waitUntil(hasText("1 файл\nЗагружено"));
        basePageSteps.onCabinetReportValidatorPage().button("Проверить").click();

        basePageSteps.onCabinetReportValidatorPage().validationResult().results().should(hasSize(3));
        basePageSteps.onCabinetReportValidatorPage().validationResult().should(hasText("Ошибки · 15" +
                "\nПредупреждения · 3\nСервис\nreport.json\nНе валидное значение VIN\n1\nСервис\n" +
                "report.xml\nНе валидное значение VIN\n1\nДоп. оборудование\nreport.csv\nНе валидное " +
                "значение VIN\n1\nСтатистика\nВсе файлы\nВсего записей 3\nС ошибками 15\nС предупреждением " +
                "3\nНевалидных VIN 0\nУникальных VIN 3\nС пробегом 3\nС нулевым пробегом 0\nСоблюдается " +
                "уникальность ID ДА\nЗаписей с работами 3"));
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Валидация текста")
    public void shouldValidateText() {
        mockRule.with("cabinet/ApiV1AdminValidateTextBatch").update();

        basePageSteps.onCabinetReportValidatorPage().input("Укажите текст файла", TEXT);
        basePageSteps.onCabinetReportValidatorPage().button("Проверить").click();
        basePageSteps.onCabinetReportValidatorPage().validationResult().errors()
                .waitUntil(hasText("Не валидное значение VIN\n1"));
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Валидация файла по url")
    public void shouldValidateUrl() {
        validateByUrl();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Переключение ошибки/предупреждения")
    public void shouldSwitchWarnError() {
        validateByUrl();
        basePageSteps.onCabinetReportValidatorPage().validationResult().button("Предупреждения · 1").click();
        basePageSteps.onCabinetReportValidatorPage().validationResult().errors().waitUntil
                (hasText("Поле y должно быть в диапазоне 1950 - 2021. Значение: 1\n1"));
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение записей в блоке «Статистика VIN»")
    public void shouldSeeStatistics() {
        validateByUrl();
        basePageSteps.onCabinetReportValidatorPage().validationResult().statistics()
                .waitUntil(hasText("Статистика\nВсе файлы\nВсего записей 1\nС ошибками 0\nС предупреждением 1\n" +
                        "Невалидных VIN 0\nУникальных VIN 1\nС пробегом 1\nС нулевым пробегом 0\nСоблюдается " +
                        "уникальность ID ДА\nЗаписей с работами 1"));
    }

    @Step("Валидируем файл по url")
    public void validateByUrl() {
        mockRule.with("cabinet/ApiV1AdminValidateUrlBatch").update();

        basePageSteps.onCabinetReportValidatorPage().input("Укажите URL адрес", URL);
        basePageSteps.onCabinetReportValidatorPage().button("Проверить").click();
        basePageSteps.onCabinetReportValidatorPage().validationResult().errors().waitUntil
                (hasText("Не валидное значение VIN\n1"));
    }
}
