package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface DealFormDkpPhotoItem extends VertisElement, WithButton {

    @Name("Ссылка на ДКП")
    @FindBy("./a[contains(@class, 'DkpPhotosForm__itemAction')]")
    VertisElement dkpLink();

    @Name("Фото")
    @FindBy(".//a[contains(@class, 'DkpPhotosForm__itemImage')]")
    VertisElement photo();

}
