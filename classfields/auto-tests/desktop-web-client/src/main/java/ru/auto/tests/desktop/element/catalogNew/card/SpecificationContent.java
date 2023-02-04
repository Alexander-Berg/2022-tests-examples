package ru.auto.tests.desktop.element.catalogNew.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SpecificationContent extends VertisElement {

    @Name("Список блоков с конфигурацией")
    @FindBy(".//div[@class = 'SpecificationContent__configuration']")
    ElementsCollection<SpecificationBlock> configurationBlockList();

    @Name("Список заголовков у блоков с конфигурацией")
    @FindBy(".//h3")
    ElementsCollection<SpecificationBlock> configurationTitleBlockList();

    @Step("Получаем блок с конфигурацией с индексом  «{i}»")
    default SpecificationBlock getBlock(int i) {
        return configurationBlockList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем заголовок блока с конфигурацией с индексом  «{i}»")
    default SpecificationBlock getTitle(int i) {
        return configurationTitleBlockList().should(hasSize(greaterThan(i))).get(i);
    }

}
