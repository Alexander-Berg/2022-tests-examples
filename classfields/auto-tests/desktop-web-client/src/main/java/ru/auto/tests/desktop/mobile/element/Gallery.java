package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.gallery.Panorama;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Gallery extends VertisElement {

    @Name("Список элементов")
    @FindBy(".//div[contains(@class, 'ListingItemGallery__item')] | " +
            "./div[contains(@class, 'OfferGallery__item')] | " +
            ".//div[contains(@class, 'CardGroupImageGalleryMobile__item')] | " +
            ".//div[contains(@class, 'OfferGallery__item_type_IMAGE')]")
    ElementsCollection<GalleryItem> itemsList();

    @Step("Получаем элемент с индексом {i}")
    default GalleryItem getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Текущее фото")
    @FindBy(".//div[contains(@class, 'fotorama__active')]/img | " +
            ".//div[contains(@class, 'image-gallery-slide center')]//img | " +
            ".//img[contains(@class, 'OfferGallery__itemImage')]")
    VertisElement currentImage();

    @Name("Предупреждение, что фото из каталога")
    @FindBy("//div[contains(@class, 'itemBadgeCatalog')]")
    VertisElement photoFromCatalogWarning();

    @Name("Панорама")
    @FindBy(".//div[contains(@class, 'OfferGallery__item_type_PANORAMA_EXTERIOR')]")
    Panorama panorama();
}
