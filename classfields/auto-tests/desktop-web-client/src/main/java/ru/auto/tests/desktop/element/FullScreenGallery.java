package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithBadge;
import ru.auto.tests.desktop.element.card.gallery.Contacts;
import ru.auto.tests.desktop.element.card.gallery.VinReport;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface FullScreenGallery extends VertisElement, WithBadge, WithActivePopup {

    @Name("Текущее изображение")
    @FindBy(".//div[contains(@class,'fotorama__active')]/img | " +
            ".//div[contains(@class, 'image-gallery-slide center')]//img | " +
            ".//img[contains(@class, 'ImageGalleryFullscreenVertical__background')] | " +
            ".//img[@class = 'ImageGalleryFullscreenHorisontal__image']")
    VertisElement currentImage();

    @Name("Блок контактов")
    @FindBy("//div[contains(@class, 'ImageGallery__sideBar')] | " +
            "//div[contains(@class, 'CardImageGallerySidebar__block')]")
    Contacts contacts();

    @Name("Кнопка закрытия")
    @FindBy(".//div[contains(@class,'fotorama__fullscreen-icon')] | " +
            ".//button[contains(@class, 'ImageGallery__fullscreenButton')] | " +
            ".//*[contains(@class, 'Icon_close')] | " +
            ".//*[contains(@class, 'IconSvg_close')] | " +
            ".//button[contains(@class, '__close-button')]")
    VertisElement closeButton();

    @Name("Список превью")
    @FindBy(".//a[contains(@class, 'image-gallery-thumbnail')] | " +
            ".//div[contains(@class, 'ImageGalleryFullscreenVertical__thumb-container')] | " +
            ".//div[contains(@class, 'ImageGalleryFullscreenVertical__thumbContainer')]")
    ElementsCollection<VertisElement> thumbList();

    @Name("Видеоплеер")
    @FindBy(".//*[contains(@class, 'VideoBox__iframe')]")
    VertisElement player();

    @Name("Отчёт по автомобилю")
    @FindBy(".//div[contains(@class, 'VinReportPromo')]")
    VinReport vinReport();

    @Name("Активное изображение")
    @FindBy(".//div[contains(@class, 'ImageGalleryFullscreenVertical__thumbContainer_active')]//img")
    VertisElement imgActive();

    @Step("Получаем превью с индексом {i}")
    default VertisElement getThumb(int i) {
        return thumbList().should(hasSize(greaterThan(i))).get(i);
    }
}
