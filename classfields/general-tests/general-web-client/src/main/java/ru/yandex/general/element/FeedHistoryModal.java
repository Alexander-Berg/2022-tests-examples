package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FeedHistoryModal extends VertisElement, Popup {

    @Name("Строки в таблице истории")
    @FindBy(".//div[contains(@class, 'History__row_')]")
    ElementsCollection<FeedHistoryRow> rows();

    @Name("Блок пагинации")
    @FindBy(".//ul[contains(@class, 'Pagination__list')]")
    Paginator paginator();

    @Name("Сообщение")
    @FindBy(".//div[contains(@class, 'Message__container')]")
    VertisElement message();

    @Name("Таблица ошибок")
    @FindBy(".//div[contains(@class, '_bodyRow')]")
    ElementsCollection<FeedErrorRow> errorsTable();

    @Name("Таб «{{ value }}»")
    @FindBy(".//label[contains(@class, 'Tab__root')][contains(., '{{ value }}')]")
    Tab tab(@Param("value") String value);

    @Name("Кнопка «Назад»")
    @FindBy(".//button[contains(@class, '_backButton')]")
    VertisElement backButton();

}
