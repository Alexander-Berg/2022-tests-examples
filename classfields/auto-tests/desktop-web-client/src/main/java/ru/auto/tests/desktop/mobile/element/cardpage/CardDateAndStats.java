package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * Created by kopitsa on 18.09.17.
 */
public interface CardDateAndStats extends VertisElement {

    @Name("Счетчик «Добавлено в избранное» (сердечко)")
    @FindBy(".//div[contains(@class, '_favorites')]")
    VertisElement favoriteCounter();

    @Name("Дата")
    @FindBy(".//div[contains(@class, 'item_creationDate')]")
    VertisElement date();
}