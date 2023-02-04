package ru.yandex.realty.element.saleads.popup;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.element.ButtonWithHint;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.newbuilding.extended.SiteNameFilter;
import ru.yandex.realty.element.saleads.ByName;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.RadioButton;
import ru.yandex.realty.element.saleads.SelectButton;
import ru.yandex.realty.element.saleads.SelectionBlock;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;

public interface ExtendedFiltersPopup extends AtlasWebElement, SelectButton, SelectionBlock, RadioButton, InputField, Link {

    @Name("Кнопка «Показать»")
    @FindBy(".//div[contains(@class,'_counter-submit')][not(contains(@class,'_loading'))]//button[contains(@class, 'submit')]")
    RealtyElement applyFiltersButton();

    @Name("Кнопка «Меньше»")
    @FindBy(".//div[contains(@class, 'show-more')]")
    RealtyElement reduceFiltersButton();

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name')][contains(.//label/text(), '{{ value }}')]")
    ByName byName(@Param("value") String value);

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name')][.//label/text()='{{ value }}']")
    InputField byExactName(@Param("value") String value);

    @Name("Фильтр имени ЖК")
    @FindBy("//span[contains(@class, 'Suggest__input')]")
    SiteNameFilter buildingNameFilter();

    @Name("Саджест")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//li")
    ElementsCollection<AtlasWebElement> suggest();

    @Name("Элемент саджеста «{{ value }}»")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//span[@class='FiltersFormField__suggest-item-label'][contains(.,'{{ value }}')]")
    AtlasWebElement suggest(@Param("value") String value);

    @Name("Искать в описании объявления")
    @FindBy(".//div[contains(@class, '_includeTag')]//input")
    AtlasWebElement includeTags();

    @Name("Исключить, если в описании")
    @FindBy(".//div[contains(@class, '_excludeTag')]//input")
    AtlasWebElement excludeTags();

    @Name("Класс бизнес центра «{{ value }}»")
    @FindBy(".//button[.='{{ value }}']")
    AtlasWebElement classButton(@Param("value") String value);

    @Name("Кнопка выбора расширенных фильтров")
    @FindBy(".//div[contains(@class, 'FiltersFormField_section_extra')]/span")
    RealtyElement showMoreFiltersButton();

    @Name("Кнопка с подсказкой «{{ value }}»")
    @FindBy("//button[contains(.,'{{ value }}')]")
    ButtonWithHint buttonWithHint(@Param("value") String value);

    @Step("Жмем на кнопку «{buttonName}» для появления попапа")
    default void clickDropdown(String buttonName) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().ignoreExceptions()
                .pollInterval(1, SECONDS).atMost(10, SECONDS).then()
                .until(() -> {
                    button(buttonName).click();
                    filterPopup().waitUntil(WebElementMatchers.isDisplayed());
                    return true;
                });
    }
}
