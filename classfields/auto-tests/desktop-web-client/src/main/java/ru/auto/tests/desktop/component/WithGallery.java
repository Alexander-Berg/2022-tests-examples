package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Gallery;

public interface WithGallery {

    @Name("Галерея")
    @FindBy("//div[@class = 'gallery'] | " +
            "//div[contains(@class, 'VinReportGallery ')] | " +
            "//div[contains(@class, 'ImageGallery')] | " +
            "//div[contains(@class, 'image-gallery-content fullscreen')]")
    Gallery gallery();
}
