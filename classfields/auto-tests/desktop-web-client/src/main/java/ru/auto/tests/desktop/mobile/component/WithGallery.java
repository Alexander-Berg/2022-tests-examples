package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.FullScreenGallery;
import ru.auto.tests.desktop.mobile.element.Gallery;

public interface WithGallery {

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'fotorama_type_gallery')] | " +
            "//div[@class = 'ImageGallery'] | " +
            "//div[contains(@class, 'ListingItemGallery_size_BIG')] | " +
            "//div[contains(@class, 'OfferGallery_size_BIG')] | " +
            "//div[contains(@class, 'CardGroupImageGalleryMobile')]")
    Gallery gallery();

    @Name("Полноэкранная галерея")
    @FindBy("//div[contains(@class, 'fullScreenGallery--fullscreen')] | " +
            "//div[contains(@class, 'image-gallery fullscreen-modal')] | " +
            "//div[contains(@class, 'ImageFullscreenGallery')]")
    FullScreenGallery fullScreenGallery();
}