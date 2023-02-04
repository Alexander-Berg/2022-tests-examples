package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface CardOwnerPanel extends VertisElement, WithButton {

    String STATUS_INACTIVE = "Не опубликовано";
    String STATUS_ACTIVE = "Опубликовано";
    String PUBLISH = "Опубликовать";
    String WITHDRAW = "Снять с продажи";

    @Name("Статус объявления")
    @FindBy(".//*[contains(@class, 'status-value')] | " +
            ".//div[contains(@class, 'CardOwnerControls__statusValue')]")
    VertisElement status();
}
