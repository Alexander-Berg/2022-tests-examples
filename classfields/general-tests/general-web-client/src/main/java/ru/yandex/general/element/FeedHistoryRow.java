package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FeedHistoryRow extends VertisElement {

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'History__status_')]/span")
    VertisElement status();

    @Name("Дата")
    @FindBy(".//div[contains(@class, 'History__time_')]/span")
    VertisElement time();

    @Name("Объявления")
    @FindBy(".//div[contains(@class, 'History__cell_')][3]/span")
    VertisElement totalOffers();

    @Name("Активные")
    @FindBy(".//div[contains(@class, 'History__cell_')][4]/span")
    VertisElement activeOffers();

    @Name("Ошибки")
    @FindBy(".//div[contains(@class, 'History__cell_')][5]/span")
    VertisElement errorOffers();

    @Name("Критические")
    @FindBy(".//div[contains(@class, 'History__cell_')][6]/span")
    VertisElement criticalErrorOffers();

}
