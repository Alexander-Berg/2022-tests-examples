package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Rate extends VertisElement {

    @Name("Кнопка «Да»")
    @FindBy(".//button[contains(@class, 'ReviewRate__like')]")
    VertisElement yesButton();

    @Name("Кнопка «Нет»")
    @FindBy(".//button[contains(@class, 'ReviewRate__dislike')]")
    VertisElement noButton();
}
