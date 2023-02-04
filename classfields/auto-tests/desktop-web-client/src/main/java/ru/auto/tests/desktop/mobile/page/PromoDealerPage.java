package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.PromoDealerForm;
import ru.auto.tests.desktop.element.PromoSidebar;
import ru.auto.tests.desktop.mobile.element.PromoDealerCallbackPopup;

public interface PromoDealerPage extends PromoPage {

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, 'Sidebar')]")
    PromoSidebar promoSidebar();

    @Name("Форма")
    @FindBy("//div[contains(@class, 'FormContainer') or contains(@class, 'BasePromoPage__container') " +
            "or contains(@class, 'BecomeClientForm')]")
    PromoDealerForm form();

    @Name("Сообщение об успешной отправке")
    @FindBy("//div[contains(@class, '__successText')] | " +
            "//p[contains(@class, 'resultMessage')]")
    VertisElement successMessage();

    @Name("Презентация")
    @FindBy("//div[contains(@class, 'PageForDealersMain__info')]")
    VertisElement presentation();

    @Name("Кнопка «Стать клиентом» в презентации")
    @FindBy("//a[contains(@class, 'Button')]//span[.='{{ text }}']")
    VertisElement presentationButton(@Param("text") String Text);

    @Name("Стоимость размещения")
    @FindBy("//div[contains(@class, 'PageForDealersMain__costSection')]")
    VertisElement price();

    @Name("Продвижение объявлений")
    @FindBy("//div[contains(@class, 'PageForDealersMain__promotionSection')]")
    VertisElement services();

    @Name("Истории успеха")
    @FindBy("//div[contains(@class, 'PageForDealersMain__successSection')]")
    VertisElement successStories();

    @Name("Форма регистрации")
    @FindBy("//div[contains(@class, 'BecomeClientForm')]")
    VertisElement becomeClientBlock();

    @Name("Саджест регионов")
    @FindBy("//div[contains(@class, 'RichInput__popup')]")
    VertisElement geoSuggest();

    @Name("Инпут «{{ text }}»")
    @FindBy("//div[contains(@class, 'GeoSuggest__suggest-item-region') and .='{{ text }}']")
    VertisElement geoSuggestItem(@Param("text") String Text);

    @Name("Кнопка «Обратный звонок»")
    @FindBy("//span[contains(@id, 'callback-form-trigger')]")
    VertisElement callbackButton();

    @Name("Поп-ап обратного звонка")
    @FindBy("//div[contains(@class, 'PromoPageForDealerForm__modal')]")
    PromoDealerCallbackPopup callbackPopup();
}