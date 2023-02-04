package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.CheckButton;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;


/**
 * Created by ivanvan on 17.07.17.
 */
public interface FeatureField extends SelectionBlock, Button, CheckButton {

    int SECOND = 1;

    @Name("Список ввода")
    @FindBy(".//span[contains(@class, 'TextInput__box')]")
    ElementsCollection<InputField> inputList();

    @Name("Ошибка")
    @FindBy(".//div[contains(@class, 'offer-form-row__error')]")
    AtlasWebElement error();

    default void inputInItem(int i, String text) {
        inputList().should(hasSize(greaterThan(i))).get(i).input().clear();
        inputList().should(hasSize(greaterThan(i))).get(i).input().sendKeys(text);
        inputList().get(i).input().should(hasValue(text));
    }

    default RealtyElement input() {
        return inputList().waitUntil(hasSize(greaterThan(0))).get(0).input();
    }

    default RealtyElement input(int i) {
        return inputList().waitUntil(hasSize(greaterThan(i))).get(i).input();
    }
}
