package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.WrapsDriver;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface AuthPopup extends VertisElement, WrapsDriver, WithButton, WithInput {

    @Name("Айфрейм авторизации")
    @FindBy(".//iframe[@class='LoginFrame']")
    VertisElement iframe();

    @Step("Переключаемся на фрейм поп-апа авторизации")
    default void switchToAuthPopupFrame() {
        getWrappedDriver().switchTo().frame(iframe());
    }
}