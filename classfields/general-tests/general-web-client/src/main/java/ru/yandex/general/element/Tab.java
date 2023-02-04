package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Tab extends VertisElement {

    @Name("Каунтер")
    @FindBy(".//span[contains(@class, 'Count')]")
    VertisElement count();

    default int getCount() {
        return Integer.valueOf(count().getText());
    }

}
