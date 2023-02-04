package ru.auto.tests.desktop.element.forms;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithShare;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface EvaluationResult extends VertisElement, WithButton, WithRadioButton, WithShare {

    @Name("Сниппет «{{ text }}»")
    @FindBy(".//div[@class = 'EvaluationResultSnippet' and " +
            ".//div[@class = 'EvaluationResultSnippet__title' and .= '{{ text }}']]")
    EvaluationResultSnippet snippet(@Param("text") String text);

    @Name("Список объявлений")
    @FindBy("//div[@class = 'ListingItemDesktop'] | " +
            "//div[@class = 'ListingItemMobile']")
    ElementsCollection<VertisElement> salesList();

    @Step("Получаем объявление с индексом {i}")
    default VertisElement getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Поп-ап трейд-ина")
    @FindBy("//div[contains(@class, 'TradeinEvaluationForm')]")
    TradeInForm tradeInForm();
}