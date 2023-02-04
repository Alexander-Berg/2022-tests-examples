package ru.auto.tests.desktop.mobile.component.mobilereviews;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface ItemsSliderBlock extends VertisElement, WithButton {

    @Name("Элементы карусели")
    @FindBy(".//div[@class='AmpSlider__item']")
    ElementsCollection<VertisElement> items();
}
