package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Benefits extends VertisElement, WithButton {

    String ELECTROCAR = "Электромобиль";

    @Name("Преимущество {{ text }}")
    @FindBy(".//div[contains(@class, 'CardBenefits__item') and .//*[contains(., '{{ text }}')]]")
    VertisElement benefit(@Param("text") String text);

}
