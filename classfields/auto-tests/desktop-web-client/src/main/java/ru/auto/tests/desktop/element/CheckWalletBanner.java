package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CheckWalletBanner extends VertisElement {

    @Name("Заголовок баннера")
    @FindBy(".//div[@class = 'InfoBubble__title']")
    VertisElement title();

    @Name("Текст баннера")
    @FindBy(".//span")
    VertisElement text();

    @Name("Ссылка «Подробнее»")
    @FindBy(".//a[@class = 'Link']")
    VertisElement link();

    @Name("Кнопка закрытия баннера")
    @FindBy(".//div[contains(@class, 'ModalDialogCloser')] | " +
            ".//div[contains(@class, 'CloseButton')]")
    VertisElement closeButton();

    @Name("Кнопка «Пополнить кошелёк»")
    @FindBy(".//a[contains(@class, 'PlacementAutoProlongationWalletNotice__button')]")
    VertisElement addFundsButton();
}