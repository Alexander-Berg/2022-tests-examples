package ru.auto.tests.desktop.element.catalogNew.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithShare;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Gallery extends VertisElement, WithShare {

    @Name("Список превью")
    @FindBy(".//a[@class = 'Brazzers__page']")
    ElementsCollection<VertisElement> thumbList();

    @Step("Получаем превью с индексом {i}")
    default VertisElement getThumb(int i) {
        return thumbList().should(hasSize(greaterThan(i))).get(i);
    }
}
