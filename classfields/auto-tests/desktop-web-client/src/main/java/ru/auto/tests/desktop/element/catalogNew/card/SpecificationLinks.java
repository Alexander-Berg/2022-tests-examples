package ru.auto.tests.desktop.element.catalogNew.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SpecificationLinks extends VertisElement {

    @Name("Активная ссылка")
    @FindBy(".//div[@class = 'SpecificationLinks__linkWrapper_active']")
    VertisElement activeLink();

    @Name("Заголовок блока h2")
    @FindBy(".//h2")
    VertisElement h2();

    @Name("Список ссылок")
    @FindBy(".//div[@class = 'SpecificationLinks__linkWrapper']")
    ElementsCollection<VertisElement> linkList();

    @Step("Получаем ссылку с индексом  «{i}»")
    default VertisElement getLink(int i) {
        return linkList().should(hasSize(greaterThan(i))).get(i);
    }

}
