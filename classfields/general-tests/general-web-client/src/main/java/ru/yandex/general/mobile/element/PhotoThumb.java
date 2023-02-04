package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PhotoThumb extends VertisElement {

    @Name("Кнопка поворота")
    @FindBy(".//button[contains(@class, '_thumbButton')][1]")
    VertisElement rotate();

    @Name("Кнопка удаления")
    @FindBy(".//button[contains(@class, '_thumbButton')][2]")
    VertisElement delete();

}
