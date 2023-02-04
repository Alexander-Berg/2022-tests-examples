package ru.auto.tests.desktop.element.compare;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Model extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//span[contains(@class, 'ComparableModelHeadCell__title')]")
    VertisElement title();

    @Name("Ссылка")
    @FindBy(".//a[contains(@class, 'ComparableModelHeadCell__link')]")
    VertisElement url();

    @Name("Кнопка «Удалить из сравнения»")
    @FindBy(".//div[contains(@class, 'CloseButton')]")
    VertisElement deleteButton();
}
