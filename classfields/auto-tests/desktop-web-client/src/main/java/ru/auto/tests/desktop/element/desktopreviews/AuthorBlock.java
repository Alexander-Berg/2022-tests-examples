package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AuthorBlock extends VertisElement {

    @Name("Ссылка на профиль автора отзыва")
    @FindBy(".//div[contains(@class, 'Review__userInfo')]//a")
    VertisElement authorUrl();

    @Name("Кнопка «Да» в блоке «Понравился отзыв?»")
    @FindBy(".//label[contains(@class, 'ReviewRate__button')][1]")
    VertisElement rateYesButton();

    @Name("Кнопка «Нет» в блоке «Понравился отзыв?»")
    @FindBy(".//label[contains(@class, 'ReviewRate__button')][2]")
    VertisElement rateNoButton();
}
