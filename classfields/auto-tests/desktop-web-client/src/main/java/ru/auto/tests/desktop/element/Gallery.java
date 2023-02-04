package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithBadge;
import ru.auto.tests.desktop.element.card.gallery.Panorama;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Gallery extends VertisElement, WithBadge {

    @Name("Кнопка перехода на предыдущее фото")
    @FindBy(".//div[contains(@class, 'ImageGalleryDesktop__left-nav')]")
    VertisElement prevButton();

    @Name("Кнопка перехода на следующее фото")
    @FindBy(".//div[contains(@class, 'ImageGalleryDesktop__right-nav')]")
    VertisElement nextButton();

    @Name("Текущее фото")
    @FindBy(".//div[contains(@class,'fotorama__active')]/img | " +
            "//div[contains(@class, 'image-gallery-slide center')]//img[@class = 'image-gallery-image'] |" +
            ".//img[contains(@class, 'ImageGalleryDesktop__image') and not(contains(@class, 'hidden'))]")
    VertisElement currentImage();

    @Name("Список превью")
    @FindBy(".//a[contains(@class,'gallery__thumb-item')] | " +
            ".//a[contains(@class, 'image-gallery-thumbnail')] | " +
            ".//img[contains(@class, 'ImageGalleryDesktop__thumb')]")
    ElementsCollection<VertisElement> thumbList();

    @Step("Получаем превью с индексом {i}")
    default VertisElement getThumb(int i) {
        return thumbList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Видеоплеер")
    @FindBy(".//*[@id='fotoramaPlayer'] | " +
            ".//*[contains(@class, 'VideoBox__iframe')]")
    VertisElement player();

    @Name("Предупреждение, что фото из каталога")
    @FindBy(".//div[contains(@class, 'CardImageGallery__fakeWarning')]")
    VertisElement photoFromCatalogWarning();

    @Name("Фрейм панорамы Spincar")
    @FindBy(".//iframe[contains(@class, 'ImageGalleryDesktop__spincar')]")
    VertisElement spincarFrame();

    @Name("Панорама экстерьера")
    @FindBy(".//div[contains(@class, 'PanoramaExterior')]")
    Panorama panoramaExterior();

    @Name("Панорама")
    @FindBy(".//div[contains(@class, 'PanoramaInterior')]")
    VertisElement panoramaInterior();

    @Name("Ошибка панорамы")
    @FindBy(".//a[contains(@class, 'PanoramaProcessingError')]")
    VertisElement panoramaError();

    @Name("Список цветов")
    @FindBy(".//div[contains(@class, 'ColorSelectorItem')]")
    ElementsCollection<VertisElement> colorsList();

    @Step("Получаем цвет с индексом {i}")
    default VertisElement getColor(int i) {
        return colorsList().should(hasSize(greaterThan(i))).get(i);
    }
}
