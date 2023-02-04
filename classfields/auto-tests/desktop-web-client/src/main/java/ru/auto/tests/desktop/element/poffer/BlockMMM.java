package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface BlockMMM extends VertisElement {

    @Name("Марка '{{ name }}'")
    @FindBy(".//div[. = '{{ name }}']")
    VertisElement mark(@Param("name") String name);

    @Name("Модель '{{ name }}'")
    @FindBy(".//div[. = '{{ name }}']")
    VertisElement model(@Param("name") String name);

    @Name("Список марок")
    @FindBy(".//div[contains(@class, 'marks-list__list_visible')]" +
            "//div[contains(@class, 'marks-list__item')]")
    ElementsCollection<VertisElement> marksList();

    @Name("Список моделей")
    @FindBy(".//div[contains(@class, 'models-list__list_visible')]" +
            "//div[contains(@class, 'models-list__item')]")
    ElementsCollection<VertisElement> modelList();

    @Name("Список параметров в блоке '{{ name }}'")
    @FindBy(".//div[contains(@class, 'visible') and ./div[.= '{{ name }}']]//label")
    ElementsCollection<VertisElement> itemsList(@Param("name") String name);

    @Name("Список кузовов на форме")
    @FindBy(".//div[contains(@class, 'bodies_visible')]//label[contains(@class, 'radio_type_body')]")
    ElementsCollection<VertisElement> bodyList();

    @Name("Список марок с фильтром")
    @FindBy(".//div[contains(@class, 'marks-list__filter-list_visible')]" +
            "//div[contains(@class, 'marks-list__item')]")
    ElementsCollection<VertisElement> marksFilteredList();

    @Name("Список моделей с фильтром")
    @FindBy(".//div[contains(@class, 'models-list__filter-list_visible')]" +
            "//div[contains(@class, 'models-list__item')]")
    ElementsCollection<VertisElement> modelsFilteredList();

    @Step("Получаем марку индексом {i}")
    default VertisElement getMark(int i) {
        return marksList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем модель индексом {i}")
    default VertisElement getModel(int i) {
        return modelList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем кузов индексом {i}")
    default VertisElement getBody(int i) {
        return bodyList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем параметр с индексом {i} в блоке {name}")
    default VertisElement getItem(String name, int i) {
        return itemsList(name).should(hasSize(greaterThan(i))).get(i);
    }
}
