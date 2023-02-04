package ru.auto.tests.desktop.element.card.gallery;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Panorama extends VertisElement, WithButton {

    @Name("Промо точек")
    @FindBy(".//div[contains(@class, 'PanoramaHotSpotsPromo')]")
    PanoramaSpotsPromo spotsPromo();

    @Name("Кнопка показа/скрытия точек")
    @FindBy(".//div[contains(@class, 'PanoramaHotSpotsModeSwitcher')]")
    VertisElement showHideSpotsButton();

    @Name("Список точек")
    @FindBy(".//div[contains(@class, 'PanoramaHotSpot') and contains(@style, 'calc') and not(contains(@style, '-100%'))]")
    ElementsCollection<VertisElement> spotsList();

    @Step("Получаем точку с индексом {i}")
    default VertisElement getSpot(int i) {
        return spotsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Поп-ап с фото точки")
    @FindBy("//parent::body//div[contains(@class, 'PanoramaHotSpotsFullViewModal')]")
    PanoramaSpotPhotoPopup spotPhotoPopup();

    @Name("Поп-ап редактирования точки")
    @FindBy("//parent::body//div[contains(@class, 'PanoramaHotSpotsCommentPopup')]")
    PanoramaSpotEditPopup spotEditPopup();

    @Name("Поп-ап добавления точки")
    @FindBy("//parent::body//div[contains(@class, 'PanoramaHotSpotsContextMenu')]")
    PanoramaSpotAddPopup spotAddPopup();

    @Name("Двигающийся указатель")
    @FindBy(".//div[contains(@class, 'PanoramaPressAndSpin')]")
    VertisElement pointer();
}