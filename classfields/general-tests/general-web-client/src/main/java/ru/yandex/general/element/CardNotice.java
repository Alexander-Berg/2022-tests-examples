package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardNotice extends VertisElement, Input, Button {

    String SAVE = "Сохранить";

    @Name("Иконка корзины")
    @FindBy(".//*[contains(@class, '_iconTrash')]")
    VertisElement trash();

}
