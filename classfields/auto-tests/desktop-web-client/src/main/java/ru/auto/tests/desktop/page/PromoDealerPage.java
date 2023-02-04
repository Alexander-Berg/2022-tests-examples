package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.element.PromoDealerCallbackPopup;
import ru.auto.tests.desktop.element.PromoDealerForm;

public interface PromoDealerPage extends PromoPage, WithGeoSuggest, WithButton {

    @Name("Форма")
    @FindBy("//div[contains(@class, 'FormContainer') or contains(@class, 'BasePromoPageForm') " +
            "or contains(@class, 'BecomeClientForm')]")
    PromoDealerForm form();

    @Name("Сообщение об успешной отправке")
    @FindBy("//div[contains(@class, '__successText')] | " +
            "//p[contains(@class, 'resultMessage')]")
    VertisElement successMessage();

    @Name("Стоимость размещения")
    @FindBy("//div[contains(@class, 'PageForDealersMain__costSection')]")
    VertisElement price();

    @Name("Продвижение объявлений")
    @FindBy("//div[contains(@class, 'PageForDealersMain__promotionSection')]")
    VertisElement services();

    @Name("Форма регистрации")
    @FindBy("//div[contains(@class, 'BecomeClientForm')]")
    VertisElement becomeClientBlock();

    @Name("Кнопка «Обратный звонок»")
    @FindBy("//span[contains(@id, 'callback-form-trigger')]")
    VertisElement callbackButton();

    @Name("Поп-ап обратного звонка")
    @FindBy("//div[contains(@class, 'PromoPageForDealerForm__modal')]")
    PromoDealerCallbackPopup callbackPopup();
}