package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.gallery.CreditPopup;
import ru.auto.tests.desktop.mobile.element.gallery.Panorama;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FullScreenGallery extends VertisElement, WithButton {

    @Name("Счётчик элементов")
    @FindBy("//div[contains(@class, 'CardGallery__topColumn2')] | " +
            ".//div[contains(@class, 'ImageFullscreenGallery__controlCenter')]")
    VertisElement counter();

    @Name("Список элементов")
    @FindBy("//div[contains(@class, 'ImageFullscreenGallery__item')]")
    ElementsCollection<FullscreenGalleryItem> itemsList();

    @Step("Получаем элемент с индексом {i}")
    default FullscreenGalleryItem getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Текущее фото")
    @FindBy(".//div[contains(@class, 'fotorama__active')]/img | " +
            ".//img[contains(@class, 'image-gallery-image')] | " +
            ".//img[contains(@class, 'ImageFullscreenGallery__image')]")
    VertisElement currentImage();

    @Name("Иконка закрытия")
    @FindBy(".//*[contains(@class, 'IconSvg_close')]")
    VertisElement closeIcon();

    @Name("Кнопка «Позвонить»")
    @FindBy(".//div[contains(@class, 'CardGallery__bottomRight')]/button[.//*[contains(@class, 'IconSvg_phone2')]]")
    VertisElement callButton();

    @Name("Кнопка чата")
    @FindBy(".//div[contains(@class, 'CardGallery__bottomRight')]//button[.//*[contains(@class, 'IconSvg_chat-active')]]")
    VertisElement chatButton();

    @Name("Фрэйм воспроизведения видео")
    @FindBy("//iframe[contains(@class, 'VideoBox')]")
    VertisElement videoFrame();

    @Name("Кнопка «Добавить в избранное»")
    @FindBy(".//button[.//*[contains(@class, 'IconSvg_favorite Icon')]] | " +
            ".//a[.//*[contains(@class, 'IconSvg_favorite Icon')]]")
    VertisElement addToFavoritesButton();

    @Name("Кнопка «Удалить из избранного»")
    @FindBy(".//button[.//*[contains(@class, 'IconSvg_favorite-active')]]")
    VertisElement deleteFromFavoritesButton();

    @Name("Кредитный поп-ап")
    @FindBy("//div[contains(@class, 'DealerCreditGalleryBlock__formModal_mobile')]")
    CreditPopup creditPopup();

    @Name("Панорама")
    @FindBy(".//div[contains(@class, 'PanoramaExteriorBase')]")
    Panorama panorama();
}
