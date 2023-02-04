package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.not;

public interface MMMFilter extends VertisElement {

    @Name("Селект категорий")
    @FindBy(".//div[contains(@class, 'Select ') and .//span[.= 'Тип']]//button")
    VertisElement categorySelect();

    @Name("Селект марок")
    @FindBy(".//div[contains(@class, 'Select ') and .//input[@name = 'mark']]//button | " +
            ".//div[contains(@class, 'Select ') and .//span[.= 'Марка']]//button")
    VertisElement markSelect();

    @Name("Селект моделей")
    @FindBy(".//div[contains(@class, 'Select ') and .//input[@name = 'model']]//button | " +
            ".//div[contains(@class, 'Select ') and .//span[.= 'Модель']]//button")
    VertisElement modelSelect();

    @Name("Селект поколений")
    @FindBy(".//div[contains(@class, 'MMMFilter__item')][3]//button | " +
            ".//div[contains(@class, 'Select ') and .//span[.= 'Поколение']]//button | " +
            ".//div[contains(@class, 'Select_mode_radio-check')][3]//button")
    VertisElement generationSelect();

    @Name("Селект кузовов")
    @FindBy(".//div[contains(@class, 'Select_mode_radio-check')][4]//button")
    VertisElement bodySelect();

    @Name("Селект комплектаций")
    @FindBy(".//div[contains(@class, 'Select_mode_radio-check')][5]//button")
    VertisElement complectationSelect();

    @Name("Выпадающий список")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Dropdown dropdown();

    @Name("Поп-ап поколений")
    @FindBy("//div[@class = 'GenerationFilter__popupContent']")
    GenerationsPopup generationsPopup();

    @Step("Выбираем категорию {category}")
    default void selectCategory(String category) {
        categorySelect().should(WebElementMatchers.isDisplayed()).click();
        dropdown().item(category).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Выбираем марку {mark}")
    default void selectMark(String mark) {
        markSelect().should(WebElementMatchers.isDisplayed()).click();
        dropdown().item(mark).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Выбираем модель {model}")
    default void selectModel(String model) {
        modelSelect().waitEnabled().click();
        dropdown().item(model).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Выбираем шильд {nameplate} у модели {model}")
    default void selectNameplate(String nameplate, String model) {
        modelSelect().waitEnabled().click();
        dropdown().nameplateButton(model).waitEnabled().click();
        dropdown().item(nameplate).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Выбираем поколение {gen} в поп-апе")
    default void selectGenerationInPopup(String gen) {
        generationSelect().waitEnabled().click();
        generationsPopup().generationItem(gen).waitUntil(WebElementMatchers.isDisplayed()).click();
    }

    @Step("Выбираем поколение {gen} в выпадающем списке")
    default void selectGenerationInDropdown(String gen) {
        generationSelect().waitEnabled().click();
        dropdown().item(gen).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Сбрасываем поколение")
    default void resetGeneration() {
        generationSelect().waitEnabled().click();
        generationsPopup().resetButton().waitUntil(WebElementMatchers.isDisplayed()).click();
        generationsPopup().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Выбираем кузов {body}")
    default void selectBody(String body) {
        bodySelect().waitEnabled().click();
        dropdown().item(body).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }

    @Step("Выбираем комплектацию {complectation}")
    default void selectComplectation(String complectation) {
        complectationSelect().waitEnabled().click();
        dropdown().waitUntil(WebElementMatchers.isDisplayed());
        dropdown().item(complectation).waitUntil(WebElementMatchers.isDisplayed()).click();
        dropdown().waitUntil(not(WebElementMatchers.isDisplayed()));
    }
}
