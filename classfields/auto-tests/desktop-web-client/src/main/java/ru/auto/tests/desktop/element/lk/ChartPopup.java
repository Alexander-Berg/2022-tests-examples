package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;


public interface ChartPopup extends VertisElement {

    @Name("Иконка «Добавлено в избранное» (сердечко)")
    @FindBy(".//*[contains(@class, 'IconSvg_favorite')]")
    VertisElement favoriteIcon();
}