package ru.yandex.realty.element.subscriptions;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

public interface SubscriptionItem extends RealtyElement, Link {

    @Name("Кнопка активации рассылки")
    @FindBy(".//div[contains(@class, 'SubscriptionTile__activeToggler') and contains(@class,'Tumbler')]")
    AtlasWebElement tumbler();

    @Name("Кнопка  «Отправить ещё раз»")
    @FindBy(".//span[contains(@class, 'SubscriptionTile__resendConfirmationAction')]")
    AtlasWebElement resendConfirmButton();

    default void checkTumbler() {
        tumbler().should(not(isChecked())).click();
        tumbler().should(isChecked());
    }

    default void unCheckTumbler() {
        tumbler().should(isChecked()).click();
        tumbler().should(not(isChecked()));
    }
}
