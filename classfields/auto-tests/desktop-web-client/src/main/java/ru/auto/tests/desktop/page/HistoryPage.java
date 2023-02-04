package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAuthPopup;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.element.history.BoughtReport;
import ru.auto.tests.desktop.element.history.Contacts;
import ru.auto.tests.desktop.element.history.Sidebar;
import ru.auto.tests.desktop.element.history.TopBlock;
import ru.auto.tests.desktop.element.history.VinReport;
import ru.auto.tests.desktop.element.history.VinReportExample;
import ru.auto.tests.desktop.element.history.VinReportPreview;
import ru.auto.tests.desktop.element.history.VinReportPromo;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface HistoryPage extends BasePage, WithContactsPopup, WithAuthPopup {

    @Name("Верхний блок")
    @FindBy("//div[contains(@class, 'ProAutoLandingDesktop__top')]")
    TopBlock topBlock();

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, 'ProAutoLandingDesktop__left')] | " +
            "//div[contains(@class, 'HistoryByVin__left_visible')]")
    Sidebar sidebar();

    @Name("Промо отчёта по VIN")
    @FindBy("//div[contains(@class, 'HistoryByVinPromoBlock')]")
    VinReportPromo vinReportPromo();

    @Name("Пример отчёта по VIN")
    @FindBy("//div[contains(@class, 'Curtain__container')]")
    VinReportExample vinReportExample();

    @Name("Превью отчёта")
    @FindBy("//div[contains(@class, 'VinReportPreviewDesktop')]")
    VinReportPreview vinReportPreview();

    @Name("Отчёт по VIN")
    @FindBy("//div[contains(@class, 'HistoryByVin__right')]")
    VinReport vinReport();

    @Name("Ошибка")
    @FindBy("//div[@class = 'HistoryByVin__error']")
    VertisElement error();

    @Name("Блок контактов")
    @FindBy("//div[contains(@class, 'HistoryOfferContacts HistoryByVin__left')] | " +
            "//div[contains(@class, 'HistoryByVin__left')]")
    Contacts contacts();

    @Name("Список купленных отчётов")
    @FindBy(".//div[@class = 'MyVinReportDesktopItem']")
    ElementsCollection<BoughtReport> boughtReportsList();

    @Step("Получаем купленный отчёт с индексом {i}")
    default BoughtReport getBoughtReport(int i) {
        return boughtReportsList().should(hasSize(greaterThan(i))).get(i);
    }

}
