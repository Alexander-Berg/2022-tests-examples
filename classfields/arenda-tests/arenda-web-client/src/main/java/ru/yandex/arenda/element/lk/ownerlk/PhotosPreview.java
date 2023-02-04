package ru.yandex.arenda.element.lk.ownerlk;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.ArendaElement;

public interface PhotosPreview extends ArendaElement {

    @Name("Иконка удаления фото")
    @FindBy(".//button[contains(@class,'ImageUploaderImagePreviewControls__deleteButton')]")
    AtlasWebElement deleteButton();
}
