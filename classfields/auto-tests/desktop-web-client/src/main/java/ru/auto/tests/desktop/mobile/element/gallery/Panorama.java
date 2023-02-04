package ru.auto.tests.desktop.mobile.element.gallery;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.card.gallery.PanoramaSpotsPromo;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Panorama extends VertisElement, WithButton {

    @Name("Промо точек")
    @FindBy("//div[contains(@class, 'PanoramaExteriorHotSpotsPromo')]")
    PanoramaSpotsPromo spotsPromo();

    @Name("Список точек")
    @FindBy("//div[@class='PanoramaHotSpotsMobile__spot']")
    ElementsCollection<VertisElement> spotsList();

    @Step("Получаем точку с индексом {i}")
    default VertisElement getSpot(int i) {
        return spotsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Текст точки")
    @FindBy("//div[contains(@class, 'PanoramaHotSpotsTextView__comment')] | " +
            "//div[contains(@class, 'PanoramaHotSpotsPhotoView__comment')]")
    VertisElement spotText();

    @Name("Иконка закрытия точки")
    @FindBy("//div[contains(@class, 'PanoramaHotSpotsTextView__closer')]")
    VertisElement spotCloseIcon();

    @Name("Фото точки")
    @FindBy("//div[contains(@class, 'ImageFullscreenGallery__item')]//img[contains(@class, 'ImagePinchZoom__image')]")
    VertisElement spotPhoto();

    @Name("Показать/скрыть точки")
    @FindBy("//div[contains(@class, 'PanoramaControl')]")
    VertisElement showHideSpotsButton();
}
