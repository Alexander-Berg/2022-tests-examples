package ru.yandex.webmaster.tests;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.webmaster.tests.element.ClickIfElement;
import ru.yandex.webmaster.tests.element.Row;

public interface WebmasterPage extends WebPage {

    @Name("Загрузить фид")
    @FindBy(".//button[contains(@class,'cnAdminFeedsContent-UploadedSourcesAdd')]")
    AtlasWebElement addButton();

    @Name("Крестик закрытия попапа подписок")
    @FindBy(".//div[contains(@class,'popup_visibility_visible')]//i")
    ClickIfElement popupCloseCross();

    @Name("Строчки фидов")
    @FindBy(".//tr[contains(@class,'StarTable-Row')]")
    ElementsCollection<Row> rows();

    @Name("Инпут загрузки")
    @FindBy(".//div[@class='SerpFeedsAddPopup-Source']//input")
    AtlasWebElement inputFeeds();

    @Name("Кнопка «Готово»")
    @FindBy(".//div[@class='MessageBox-Content']//button[(contains(.,'Готово'))]")
    AtlasWebElement doneButton();

    @Name("Дропдаун региона ")
    @FindBy(".//span[@class='RegionDropdown']//button")
    AtlasWebElement region();

    @Name("Чекбокс Россия")
    @FindBy(".//span[contains(@class,'RegionPicker-RowChecker') and contains(.,'Россия')]")
    AtlasWebElement russiaRegionCheckbox();

    @Name("")
    @FindBy(".//div[contains(@class,'SerpFeedsAddPopup') and contains(@class,'Modal_visible')]")
    AtlasWebElement feedPopup();
}
