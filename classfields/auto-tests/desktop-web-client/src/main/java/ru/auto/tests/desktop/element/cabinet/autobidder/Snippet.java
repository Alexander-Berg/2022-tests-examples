package ru.auto.tests.desktop.element.cabinet.autobidder;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Snippet extends VertisElement {

    @Name("Название-ссылка")
    @FindBy(".//a[contains(@class, '_mmm')]")
    VertisElement title();

    @Name("Пробег")
    @FindBy(".//span[contains(@class, '_run')]")
    VertisElement run();

    @Name("Модификация")
    @FindBy(".//span[contains(@class, '_modification')]")
    VertisElement modification();

    @Name("Цена")
    @FindBy(".//div[contains(@class, '_price')]")
    VertisElement price();

    @Name("Дней на складе")
    @FindBy(".//div[contains(@class, '_daysInStock')]")
    VertisElement daysInStock();

    @Name("Дней без звонков")
    @FindBy(".//div[contains(@class, '_daysWithoutCalls')]")
    VertisElement daysWithoutCalls();

    @Name("Процент интереса")
    @FindBy(".//div[contains(@class, '_attentionInfo')]")
    VertisElement attentionInfo();

    @Name("Прогноз ставки")
    @FindBy(".//div[contains(@class, '_forecast')]")
    VertisElement forecast();

    @Name("Не хватает до макс. интереса")
    @FindBy(".//div[contains(@class, '_deficit')]")
    VertisElement deficit();

    @Name("Список цветных сегментов в проценте интереса")
    @FindBy(".//div[contains(@class, 'AuctionUsedRange__item_')]")
    ElementsCollection<VertisElement> colorRangeItems();

}
