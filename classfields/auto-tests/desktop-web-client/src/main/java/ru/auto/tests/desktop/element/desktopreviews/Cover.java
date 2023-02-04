package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Cover extends VertisElement {

    @Name("Кнопка «N комментариев»")
    @FindBy(".//button[contains(@class, 'Review__commentsButton')]")
    VertisElement commentsButton();

    @Name("Счётчик просмотров")
    @FindBy(".//div[@class = 'Review__firstline']")
    VertisElement counter();

    @Step("Получаем счётчик просмотров")
    default int getCounter() {
        String text = counter().getText();
        return Integer.parseInt(text.substring(text.lastIndexOf("\n") + 1));
    }
}