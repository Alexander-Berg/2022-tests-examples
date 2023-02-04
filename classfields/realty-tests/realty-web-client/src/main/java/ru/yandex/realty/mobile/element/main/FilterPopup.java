package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.mobile.element.ButtonWithText;

import static org.hamcrest.Matchers.hasSize;

public interface FilterPopup extends AtlasWebElement, ButtonWithText, FieldBlock, InputField {

    String SHOW = "Показать";
    String CANCEL = "Отменить";

    String PER_METER = "м²";
    String PER_OFFER = "за всё";
    String PER_ARE = "за сот.";

    String METRO = "Метро";
    String RAYON = "Районы";

    @Name("Регион «{{ value }}»")
    @FindBy(".//li/span[contains(@class, 'Suggest__item') and contains(.,'{{ value }}')]")
    AtlasWebElement item(@Param("value") String value);

    @Name("Список регионов на выбор")
    @FindBy(".//li/span[contains(@class, 'Suggest__item')]")
    ElementsCollection<AtlasWebElement> regionsToChoose();

    @Name("Выбранный регион")
    @FindBy(".//li/div[contains(@class, 'Suggest__item')]")
    AtlasWebElement selectedRegion();

    @Name("Метро")
    @FindBy(".//span[contains(@class, 'MetroStation__title') and contains(., '{{ value }}')]")
    AtlasWebElement metro(@Param("value") String value);

    @Name("Район")
    @FindBy(".//div[contains(@class, 'GeoRefinementSelector__list-item-label') and contains(., '{{ value }}')]")
    AtlasWebElement rayon(@Param("value") String value);

    @Name("Кнопка «Показать»")
    @FindBy(".//button[contains(@class,'Button_view_yellow')]")
    AtlasWebElement showButton();

    @Name("Стрелка назад")
    @FindBy(".//i[contains(@class,'FiltersFormFieldPanel__close')]")
    AtlasWebElement backArrow();

    default String selectFirstRegion() {
        String region = regionsToChoose().should(hasSize(3)).get(0).getText();
        regionsToChoose().should(hasSize(3)).get(0).click();
        return region;
    }
}
