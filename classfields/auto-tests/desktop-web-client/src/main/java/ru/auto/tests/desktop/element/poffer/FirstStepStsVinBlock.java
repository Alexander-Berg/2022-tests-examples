package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FirstStepStsVinBlock extends Block {

    @Name("Фото СТС")
    @FindBy(".//input[@type = 'file']")
    VertisElement stsPhoto();

    @Name("Икнока ?")
    @FindBy(".//div[contains(@class, 'FirstStepVin__popup')]")
    VertisElement helpIcon();
}