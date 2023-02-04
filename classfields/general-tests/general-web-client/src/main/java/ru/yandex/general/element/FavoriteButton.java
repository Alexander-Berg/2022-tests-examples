package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

public interface FavoriteButton extends VertisElement {

    @Name("Иконка")
    @FindBy("./*[contains(@class, 'favoriteIcon')]")
    VertisElement favoriteIcon();

    default void waitUntilChecked() {
        favoriteIcon().waitUntil(hasClass(containsString("_checked")));
    }

}
