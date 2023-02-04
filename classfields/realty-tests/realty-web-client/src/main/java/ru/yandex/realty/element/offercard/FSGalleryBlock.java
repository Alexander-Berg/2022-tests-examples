package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.element.Link;

public interface FSGalleryBlock extends Link, ButtonWithTitle, PhoneBlock {

    @Name("Кнопка «Добавить в избранное» в галерее")
    @FindBy(".//button[contains(@class,'OfferGallerySnippet__favoriteAction')]")
    AtlasWebElement addToFavButtonGallery();

    @Name("Закрыть галерею")
    @FindBy(".//button[contains(@class,'FSGalleryClose')]")
    AtlasWebElement closeButton();

    @Name("Тумбы фотографий")
    @FindBy(".//div[@class = 'GalleryThumbsThumb']")
    ElementsCollection<AtlasWebElement> galleryThumbs();

    @Name("Главный слайдер")
    @FindBy(".//div[@class = 'GallerySlide']")
    MainSlider mainSlider();
}
