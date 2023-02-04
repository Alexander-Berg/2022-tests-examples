package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.FullScreenGallery;

public interface WithFullScreenGallery {

    @Name("Полноэкранная галерея")
    @FindBy("//div[contains(@class, 'ImageGalleryFullscreenVertical')] | " +
            "//div[contains(@class, 'fotorama--fullscreen')] | " +
            "//div[contains(@class, 'image-gallery fullscreen-modal')] | " +
            "//div[contains(@class, 'ImageGalleryFullscreenHorisontal')] | " +
            "//div[contains(@class, 'ImageFullscreenGallery')]")
    FullScreenGallery fullScreenGallery();
}
