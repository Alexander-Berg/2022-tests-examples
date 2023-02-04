package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FloatedHeader extends VertisElement, Link {

    String SAVE = "Сохранить";

    @Name("Аватар")
    @FindBy(".//img[contains(@class, 'Avatar')]")
    VertisElement avatar();

    @Name("Лого «Объявления»")
    @FindBy(".//a[contains(@class, 'Link HeaderLogo')]")
    VertisElement oLogo();

    @Name("Лого «Яндекс»")
    @FindBy(".//a[contains(@class, 'yaLink')]")
    VertisElement yLogo();

    @Name("Серч-бар")
    @FindBy("//div[contains(@class, '_searchSuggestWrapper')]")
    SearchBar searchBar();

}
