package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;

public interface WalletTiedCardsBlock extends VertisElement, WithSelect, WithCheckbox {

    @Name("Кнопка добавления карты")
    @FindBy(".//button[contains(@class, 'bindCardButton')]")
    VertisElement addCardButton();

    @Name("Кнопка удаления карты")
    @FindBy(".//div[contains(@class, 'MyWalletCards__unbindCardIcon')]")
    VertisElement removeCardButton();
}