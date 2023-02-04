package ru.auto.tests.desktop.element.cabinet.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithSelect;

public interface FavoriteDiscountsCurtain extends VertisElement, WithButton, WithInput, WithSelect {

    @Name("Пример сообщения пользователю")
    @FindBy(".//div[contains(@class, 'FavoriteDiscountsCurtain__dumbContainer')]")
    VertisElement messageExample();
}
