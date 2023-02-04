package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Popup;

public interface VinHistoryCommentsPopup extends Popup {

    @Name("Иконка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'Modal__closer')]")
    VertisElement closeIcon();
}