package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Wrapper extends Input, Link, Button, FiltersPopup {

    String FIND = "Найти";
    String DISTRICTS = "Районы";
    String DONE = "Готово";

    @Name("Айтем саджеста «{{ value }}»")
    @FindBy(".//div[contains(@class,'_menuItem_')][contains(.,'{{ value }}')]")
    VertisElement suggestItem(@Param("value") String value);

    @Name("Элемент саджеста «{{ value }}» с категорией")
    @FindBy(".//div[contains(@class, 'SuggestText__withCategory')][contains(., '{{ value }}')]")
    VertisElement suggestItemWithCategory(@Param("value") String value);

    @Name("Элемент саджеста «{{ value }}» без категории")
    @FindBy(".//mark[contains(@class, 'ItemSuggestText')][contains(., '')]")
    VertisElement suggestItemWithoutCategory(@Param("value") String value);

    @Name("Список элементов саджеста")
    @FindBy("//div[contains(@class, '_menuItem_')]")
    ElementsCollection<VertisElement> suggestList();

    @Name("Очистка поискового инпута")
    @FindBy(".//button[contains(@class, 'ClearButton')]")
    VertisElement searchClearButton();

    default int getZindex() {
        return Integer.valueOf(getAttribute("style").replaceAll("\\D+", ""));
    }

}
