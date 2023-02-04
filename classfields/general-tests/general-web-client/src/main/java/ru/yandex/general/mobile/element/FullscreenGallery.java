package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Image;

public interface FullscreenGallery extends VertisElement {

    @Name("Видео")
    @FindBy(".//div[contains(@class, '_videoContainer')]")
    VertisElement video();

    @Name("Список фото в галерее")
    @FindBy(".//div[contains(@class, 'pswp__item')]")
    ElementsCollection<Image> galleryItemList();

    @Name("Кнопка закрытия галереи")
    @FindBy(".//button[contains(@class, 'close')]")
    VertisElement close();

}
