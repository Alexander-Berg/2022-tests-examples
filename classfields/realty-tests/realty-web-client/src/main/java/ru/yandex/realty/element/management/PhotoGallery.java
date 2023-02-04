package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface PhotoGallery extends AtlasWebElement {

    @Name("Выберете файлы на компьютере")
    @FindBy(".//input")
    AtlasWebElement addPhotoInput();

    @Name("Список превьюшек")
    @FindBy(".//div[@class='preview']")
    ElementsCollection<Preview> previews();

    @Name("Кнопка {{ value }}")
    @FindBy(".//button[contains(., '{{ value }}')]//span")
    RealtyElement button(@Param("value") String value);

    default Preview preview(int i) {
        return previews().should(hasSize(greaterThan(i))).get(i);
    }
}
