package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface VerticalCarouselItem extends VertisElement {

    @Name("Ссылка")
    @FindBy(".//a")
    VertisElement url();

    @Name("Кнопка показа телефонов")
    @FindBy(".//button[contains(@class, 'ListingPremiumItem__showPhoneButton')]")
    VertisElement showPhonesButton();

    @Name("Фото")
    @FindBy(".//img")
    VertisElement photo();
}