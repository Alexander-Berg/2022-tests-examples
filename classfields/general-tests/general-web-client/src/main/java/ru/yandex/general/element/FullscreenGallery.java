package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FullscreenGallery extends VertisElement {

    String ZOOMED_IN = "pswp--zoomed-in";

    @Name("Видео")
    @FindBy(".//div[contains(@class, '_videoContainer')]")
    VertisElement video();

    @Name("Список превью фото")
    @FindBy(".//div[contains(@class, 'View__preview_')]")
    ElementsCollection<Image> previewsList();

    @Name("Превью видео")
    @FindBy(".//div[contains(@class, 'View__preview')][./img[contains(@class, '_videoPreview')]]")
    VertisElement videoPreview();

    @Name("Список фото в галерее")
    @FindBy(".//div[contains(@class, 'pswp__item')]//img")
    ElementsCollection<Image> galleryItemList();

    @Name("Активная превьюха")
    @FindBy(".//div[contains(@class, 'previewActive')]//img")
    VertisElement activePreview();

    @Name("Кнопка закрытия галереи")
    @FindBy(".//button[contains(@class, 'close')]")
    VertisElement close();

}
