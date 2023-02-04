package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.mobile.component.WithBillingPopup;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithPaymentMethodsPopup;
import ru.auto.tests.desktop.mobile.element.WithInput;
import ru.auto.tests.desktop.mobile.element.history.BoughtReport;
import ru.auto.tests.desktop.mobile.element.history.VinPackagePromo;
import ru.auto.tests.desktop.mobile.element.history.VinReport;
import ru.auto.tests.desktop.mobile.element.history.VinReportPreview;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface HistoryPage extends BasePage,
        WithInput,
        WithButton,
        WithPaymentMethodsPopup,
        WithNotifier,
        WithBillingPopup {

    String QUESTION_BUTTON_TEXT = "Где смотреть\nГосномер вы найдёте в Свидетельстве " +
            "о регистрации (СТС) в строке «Регистрационный знак», а VIN в строке «Идентификационный номер».\n" +
            "Также эти данные можно найти в Паспорте транспортного средства (ПТС) и полисе ОСАГО.";

    @Name("Кнопка «?»")
    @FindBy(".//div[contains(@class, 'VinCheckInput__questionIcon')] | " +
            ".//div[contains(@class, 'VinCheckInputAmp__questionIcon')]")
    VertisElement questionButton();

    @Name("Промо пакетов отчётов")
    @FindBy("//div[contains(@class, 'HistoryByVinPackagePromoMobile')] | " +
            "//div[contains(@class, 'HistoryByVinPackagePromoAmp')]")
    VinPackagePromo vinPackagePromo();

    @Name("Превью отчёта")
    @FindBy("//div[contains(@class, 'VinReportPreview')]")
    VinReportPreview vinReportPreview();

    @Name("Отчёт по VIN")
    @FindBy("//div[contains(@class, 'HistoryByVin__right')]")
    VinReport vinReport();

    @Name("Кнопка «Войти» на странице Проавто")
    @FindBy(".//div[.= 'Войти']//a")
    VertisElement vinLoginButton();

    @Name("Кнопка «Найти»")
    @FindBy(".//button[contains(@class, 'VinCheckSnippet__button')] | " +
            ".//button[contains(@class, 'VinCheckInput__button')] | " +
            ".//button[contains(@class, 'VinCheckInputAmp__button')]")
    VertisElement findButton();

    @Name("Сообщение об ошибке")
    @FindBy(".//div[@class = 'HistoryByVin__error']")
    VertisElement error();

    @Name("Список купленных отчётов")
    @FindBy(".//div[@class = 'MyVinReportMobileItem']")
    ElementsCollection<BoughtReport> boughtReportsList();

    @Step("Получаем купленный отчёт с индексом {i}")
    default BoughtReport getBoughtReport(int i) {
        return boughtReportsList().should(hasSize(greaterThan(i))).get(i);
    }

}
