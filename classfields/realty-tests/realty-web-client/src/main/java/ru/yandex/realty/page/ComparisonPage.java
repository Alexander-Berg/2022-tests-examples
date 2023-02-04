package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.comparison.TableOfSavedItems;
import ru.yandex.realty.element.saleads.SelectionBlock;

/**
 * Created by kopitsa on 27.06.17.
 */
public interface ComparisonPage extends BasePage {

    @Name("Список элементов для сравнения")
    @FindBy("//table[contains(@class, 'ComparisonTable__wrap')]")
    TableOfSavedItems savedItemsTable();

    @Name("Таблица для сравнения")
    @FindBy("//table")
    SelectionBlock comparisionTable();

    @Name("Сообщение «Hет элементов для сравнения»")
    @FindBy("//div[contains(@class, 'Comparison__stub')]")
    Link emptyTableMarkingMessage();
}
