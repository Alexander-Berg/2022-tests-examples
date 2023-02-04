package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Chips extends VertisElement, Button {

    @Name("Иконка сброса")
    @FindBy(".//*[contains(@class, 'iconClose')]")
    VertisElement reset();

    @Name("Стрелка влево")
    @FindBy(".//*[contains(@class, '_arrowLeft')]")
    VertisElement arrowLeft();

}
