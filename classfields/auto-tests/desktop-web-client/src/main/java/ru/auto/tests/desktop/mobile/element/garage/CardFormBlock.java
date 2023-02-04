package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.WithInput;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.not;

public interface CardFormBlock extends VertisElement, WithButton, WithRadioButton, WithInput {

    String BLACK = "040001";

    @Name("Цвет «{{ color }}»")
    @FindBy(".//div[contains(@class, 'ColorItem_{{ color }}')]")
    VertisElement color(@Param("color") String color);

    @Name("Айтем «{{ text }}»")
    @FindBy(".//div[@class = 'FormSection__ListItem'][contains(., '{{ text }}')]")
    VertisElement item(@Param("text") String text);

    @Name("Блок развернут")
    @FindBy(".//div[contains(@class, 'FormSection_opened')]")
    VertisElement opened();

    default void shouldBeOpened() {
        opened().should(WebElementMatchers.isDisplayed());
    }

    default void shouldBeMinimized() {
        opened().should(not(WebElementMatchers.isDisplayed()));
    }

}
