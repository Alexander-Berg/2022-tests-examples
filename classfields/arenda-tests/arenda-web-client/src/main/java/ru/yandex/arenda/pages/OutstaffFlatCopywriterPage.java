package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Link;

public interface OutstaffFlatCopywriterPage extends BasePage {

    String SAVE_BUTTON = "Сохранить";
    String FLAT_PHOTO_URL = "Фото квартиры";
    String TOUR_3D_URL = "3D тур";

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class,'OutstaffCopywriterForm__questionnaireField') and contains(.,'{{ value }}')]")
    Link field(@Param("value") String value);

    @Name("Превьюшки фоток")
    @FindBy(".//div[contains(@class,'ImageUploaderImagePreviewControls__wrapper')]")
    ElementsCollection<AtlasWebElement> previews();

    @Name("Описание")
    @FindBy(".//textarea[@id = 'OFFER_COPYRIGHT']")
    AtlasWebElement textarea();

    @Name("Модалка")
    @FindBy(".//div[contains(@class,'Modal_visible')]")
    AtlasWebElement modal();
}
