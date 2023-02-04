package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface BetaDescriptionBlock extends VertisElement, WithCheckbox {

    String BEATEN = "Битый или не на ходу";
    String CUSTOM = "Не растаможен";

    @Name("Поле ввода описания")
    @FindBy(".//textarea[@name='description.description']")
    VertisElement descriptionTextArea();

    @Name("Список предложений описания")
    @FindBy(".//div[@class='OfferFormDescriptionField__suggestions']")
    BetaDescriptionSuggestionsBlock suggestions();

    @Name("Распарсенные из описания опции")
    @FindBy(".//div[@class='EquipmentByDescription']")
    VertisElement parsedOptions();

    @Step("Очищаем текстовое поле")
    default void clearDescription() {
        descriptionTextArea().clear();
        descriptionTextArea().sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
    }

}