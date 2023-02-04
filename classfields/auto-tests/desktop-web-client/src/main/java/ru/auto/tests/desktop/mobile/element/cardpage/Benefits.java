package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Benefits extends VertisElement {

    @Name("Преимущество {{ text }}")
    @FindBy(".//div[contains(@class, 'CardBenefits__item') and .//*[contains(., '{{ text }}')]]")
    VertisElement benefit(@Param("text") String text);

}
