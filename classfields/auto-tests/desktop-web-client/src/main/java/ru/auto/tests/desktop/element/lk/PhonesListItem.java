package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PhonesListItem extends VertisElement {

    @Name("Иконка удаления телефона")
    @FindBy("//i[contains(@class, 'TextInput__clear_visible')]")
    VertisElement deleteIcon();
}