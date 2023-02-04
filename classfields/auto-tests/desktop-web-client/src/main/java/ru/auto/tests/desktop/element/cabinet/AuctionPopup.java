package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface AuctionPopup extends VertisElement, WithInput, WithButton {

    String FOR_CALL = "За звонок";
    String APPLY = "Применить";
    String REMOVE_FROM_AUCTION = "Снять с аукциона";

    String INTEREST_TOOLTIP_TEXT = "Процент интереса - это максимальная доля показов и звонков, которые может" +
            " получить объявление, занимая определённые позиции в поисковой выдаче.\nУвеличение ставки аукциона" +
            " повышает вероятность размещения объявления на более высокой позиции в поисковой выдаче, что может" +
            " позволить привлечь больше внимания пользователей к вашему объявлению.";

    @Name("Кнопка уменьшения ставки на шаг")
    @FindBy(".//button[contains(@class, '_buttonStep')][.//*[contains(@class, 'arrow-left')]]")
    VertisElement decreaseBid();

    @Name("Кнопка увеличения ставки на шаг")
    @FindBy(".//button[contains(@class, '_buttonStep')][.//*[contains(@class, 'arrow-right')]]")
    VertisElement increaseBid();

    @Name("Кнопка максимальной ставки")
    @FindBy(".//button[contains(@class, '_buttonMaxBid')]")
    VertisElement maxBid();

    @Name("Описание")
    @FindBy(".//div[@class = 'AuctionUsedPopupContent__note']")
    AuctionPopupDescription description();

    @Name("Точки на графике")
    @FindBy(".//div[contains(@class, '_dot_') and not(contains(@class, '_dot_hidden'))]")
    ElementsCollection<VertisElement> dots();

    @Name("Столбики на графике")
    @FindBy(".//div[contains(@class, '_graphFillsItem')]")
    ElementsCollection<GraphBar> bars();

}
