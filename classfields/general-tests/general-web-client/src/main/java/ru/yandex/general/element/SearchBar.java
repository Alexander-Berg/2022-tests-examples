package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

public interface SearchBar extends VertisElement, Button {

    String MAP_METRO_DISTRICTS = "Карта, метро, районы";
    String SHOW = "Показать";
    String FIND = "Найти";

    @Name("Инпут поиска")
    @FindBy(".//input[@type = 'text']")
    VertisElement input();

    @Name("Очистка инпута")
    @FindBy(".//button[contains(@class, 'clearButton')]")
    VertisElement clearInput();

    @Name("Очистка инпута с гео")
    @FindBy(".//button[contains(@class, '_clearMapButton')]")
    VertisElement clearGeoInput();

    @Name("Элемент саджеста «{{ value }}»")
    @FindBy("//div[contains(@class, '_menuItem_')][contains(., '{{ value }}')]")
    VertisElement suggestItem(@Param("value") String value);

    @Name("Элемент саджеста «{{ value }}» с категорией")
    @FindBy("//div[contains(@class, 'SuggestText__withCategory')][contains(., '{{ value }}')]")
    VertisElement suggestItemWithCategory(@Param("value") String value);

    @Name("Элемент саджеста «{{ value }}» без категории")
    @FindBy("//div[contains(@class, 'SearchSuggest')][contains(., '{{ value }}')][not(./div)]")
    VertisElement suggestItemWithoutCategory(@Param("value") String value);

    @Name("Саджест")
    @FindBy("//div[contains(@class, 'SearchSuggestDropdown')]")
    SuggestDropdown suggest();

    default void fillSearchInput(String searchText) {
        waitSomething(1, TimeUnit.SECONDS);
        if (input().getAttribute("value").equals(""))
            input().sendKeys(searchText);
        else {
            clearInput().click();
            input().waitUntil(hasAttribute("value", ""));
            input().sendKeys(searchText);
        }
    }
}
