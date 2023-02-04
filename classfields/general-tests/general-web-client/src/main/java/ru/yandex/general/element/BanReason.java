package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BanReason extends VertisElement {

    int FIRST = 0;
    int SECOND = 1;

    @Name("Тайтл")
    @FindBy(".//div[contains(@class, 'title')]")
    VertisElement title();

    @Name("Описание")
    @FindBy(".//p[contains(@class, 'text')]/span")
    VertisElement description();

}
