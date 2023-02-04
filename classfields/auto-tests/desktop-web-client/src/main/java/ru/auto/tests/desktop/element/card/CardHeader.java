package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithShare;
import ru.auto.tests.desktop.component.WithTradeIn;
import ru.auto.tests.desktop.element.card.header.Stats;
import ru.auto.tests.desktop.element.card.header.ToolBar;

public interface CardHeader extends VertisElement, WithShare, WithTradeIn {

    @Name("Первая строка заголовка")
    @FindBy(".//div[contains(@class, 'CardHead__topRow')]")
    VertisElement firstLine();

    @Name("Панель с кнопками")
    @FindBy(".//ul[contains(@class, 'card__actions')] | " +
            ".//span[contains(@class, 'ControlGroup')]")
    ToolBar toolBar();

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'CardGroupHeader-module__title')] | " +
            ".//h1[contains(@class, 'CardNewHead__title')] | " +
            ".//h1[contains(@class, 'CardHead__title')]")
    VertisElement title();

    @Name("Кнопка «Актуально» на актуальном объявлении")
    @FindBy(".//button[contains(@class, 'ButtonActualize') and .//div[contains(@style, '%;')]]")
    VertisElement fullyActualButton();

    @Name("Кнопка «Актуально» на частично актуальном объявлении")
    @FindBy(".//button[contains(@class, 'ButtonActualize') and .//div[not(contains(@style, 'right: 0%;'))]]")
    VertisElement partiallyActualButton();

    @Name("Кнопка «Да, продаю»")
    @FindBy(".//button[.//div[. = 'Да, продаю']]")
    VertisElement yesActualButton();

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'card__price')]/span | " +
            ".//div[contains(@class, 'PriceNewOffer')] | " +
            ".//div[contains(@class, 'PriceUsedOffer')]")
    Price price();

    @Name("Цена с НДС")
    @FindBy(".//div[contains(@class, 'CardHead__withNds')]/div")
    VertisElement priceWithNds();

    @Name("Счётчик просмотров")
    @FindBy(".//li[contains(@class, 'card__stat-item_icon_views')] | " +
            ".//div[contains(@class, 'CardHead__views')]")
    VertisElement viewsCounter();

    @Name("Предложение кредита")
    @FindBy(".//span[contains(@class, 'CreditPrice')]")
    VertisElement creditOffer();

    @Name("Заметка")
    @FindBy(".//*[contains(@class, 'CardHead__note')]")
    NoteBar noteBar();

    @Name("Статистика оффера")
    @FindBy(".//div[@class = 'CardHead__topRowLeftColumn']")
    Stats stats();

    @Name("Ссылка на безопасную сделку")
    @FindBy("//div[@class = 'CardSafeDealButton']")
    VertisElement dealLink();
}
