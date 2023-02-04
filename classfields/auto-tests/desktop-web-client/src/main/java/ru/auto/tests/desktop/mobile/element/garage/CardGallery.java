package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardGallery extends VertisElement {

    @Name("Инпут фото")
    @FindBy("//input[@type = 'file']")
    VertisElement photoInput();

    @Name("Удаление фото")
    @FindBy("//button[contains(@class, '_removeButton')]")
    VertisElement deletePhoto();

}
