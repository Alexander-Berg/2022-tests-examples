package ru.auto.tests.desktop.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardGallery extends VertisElement {

    String DREAM_CAR = "Машина мечты";

    @Name("Инпут фото")
    @FindBy("//input[@type = 'file']")
    VertisElement photoInput();

    @Name("Удаление фото")
    @FindBy("//button[contains(@class, '_removeButton')]")
    VertisElement deletePhoto();

    @Name("Бейдж «{{ text }}»")
    @FindBy(".//span[contains(@class, 'GarageBadge') and .='{{ text }}']")
    VertisElement badge(@Param("text") String text);

}
