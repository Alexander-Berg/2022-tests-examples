package ru.auto.tests.desktop.element.compare;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Sale extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'ComparableOfferHeadCell__title')]")
    VertisElement title();

    @Name("Фото")
    @FindBy(".//div[contains(@class, 'ComparableOfferHeadCell')]/a")
    VertisElement photo();

    @Name("Телефон")
    @FindBy(".//a[contains(@class, 'ComparableOfferHeadCell__phoneValue')]")
    VertisElement phone();

    @Name("Кнопка «Удалить из сравнения»")
    @FindBy(".//div[contains(@class, 'CloseButton')]")
    VertisElement deleteButton();

}