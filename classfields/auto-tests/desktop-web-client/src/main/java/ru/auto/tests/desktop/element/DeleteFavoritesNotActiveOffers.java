package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface DeleteFavoritesNotActiveOffers extends VertisElement, WithButton {

    @Name("Кнопка закрытия")
    @FindBy(".//div[contains(@class, 'CloseButton')]")
    VertisElement close();

}
