package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AutoProlongFailedBanner extends VertisElement {

    @Name("Заголовок баннера")
    @FindBy(".//div[@class = 'InfoBubble__title']")
    VertisElement title();

    @Name("Текст баннера")
    @FindBy(".//div[not(@class)]")
    VertisElement text();

    @Name("Кнопка «Повторить попытку»")
    @FindBy(".//button[contains(@class, 'PlacementAutoProlongationInactiveNotice__button')]")
    VertisElement retryButton();
}