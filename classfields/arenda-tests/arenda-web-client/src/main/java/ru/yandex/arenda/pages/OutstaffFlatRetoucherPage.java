package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Link;
import ru.yandex.arenda.element.lk.ownerlk.PhotosPreview;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface OutstaffFlatRetoucherPage extends BasePage {

    String INITIAL_PHOTOS = "Исходные фотографии квартиры";

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class,'OutstaffRetoucherForm__readonlyField') and contains(.,'{{ value }}')]")
    Link field(@Param("value") String value);

    @Name("Инпут загрузки фото")
    @FindBy(".//input[@type = 'file']")
    AtlasWebElement inputPhoto();

    @Name("Превьюшки фоток")
    @FindBy(".//div[contains(@class,'ImageUploaderImagePreview__isDraggableItem')]")
    ElementsCollection<PhotosPreview> previews();

    default PhotosPreview photosPreview(int i) {
        return previews().should(hasSize(greaterThan(i))).get(i);
    }
}
