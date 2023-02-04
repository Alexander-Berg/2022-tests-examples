package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.RealtyElement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

public interface PublishBlock extends Button {

    String ACTIVE = "Active";
    String ITEM_IS_SELECTED = "item_isSelected";
    String PRICE_HIDDEN = "price_hidden";
    String NO_PHOTO_WARNING = "Объявления без фотографий не интересны покупателям и получают меньше всего просмотров.";
    String WARNING_PATTERN = "Добавьте еще %d фотографи[ию], чтобы опубликовать объявление";

    @Name("Тайтл блока")
    @FindBy(".//h2")
    AtlasWebElement h2();

    @Name("Блок с услугой {{ value }}")
    @FindBy(".//div[contains(@class, 'publish-item_service') and contains(.,'{{ value }}')]")
    RealtyElement paySelector(@Param("value") String value);

    @Name("Табы продаж")
    @FindBy(".//li[contains(@role,'tab')][contains(.,'{{ value }}')]")
    RealtyElement sellTab(@Param("value") String value);

    @Name("Кнопка оплаты")
    @FindBy(".//button[contains(@class,'Button_view_green') and not(contains(@class,'disabled'))]")
    PayButton payButton();

    @Name("Предупреждение")
    @FindBy(".//p[contains(@class,'WarningsBlock__text')]")
    AtlasWebElement warning();

    @Name("Контент")
    @FindBy("//div[contains(@class,'publish-payselector_new-form')]")
    AtlasWebElement content();

    default void deSelectPaySelector(String value) {
        paySelector(value).waitUntil(hasClass(containsString(ITEM_IS_SELECTED))).click();
        h2().click();
        paySelector(value).waitUntil(hasClass(not(containsString(ITEM_IS_SELECTED))));
    }

    default void selectPaySelector(String value) {
        paySelector(value).waitUntil(hasClass(not(containsString(ITEM_IS_SELECTED)))).click();
        h2().click();
        paySelector(value).waitUntil(hasClass(containsString(ITEM_IS_SELECTED)));
    }
}
