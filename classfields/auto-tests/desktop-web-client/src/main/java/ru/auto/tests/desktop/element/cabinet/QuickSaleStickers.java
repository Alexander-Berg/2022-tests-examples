package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface QuickSaleStickers extends VertisElement, WithInput, WithButton {

    @Name("Стикер «{{ name }}»")
    @FindBy(".//li[contains(@class, 'ServicePopupBadges__item') and contains(., '{{ name }}')]")
    VertisElement sticker(@Param("name") String name);

    @Name("Создать свой стикер")
    @FindBy(".//span[contains(@class, 'ServicePopupBadges__input')]/button")
    VertisElement applyOwnSticker();

    @Name("Сохранить новый стикер")
    @FindBy(".//button[contains(@class, 'ServicePopupBadges__submit')]")
    VertisElement apply();
}
