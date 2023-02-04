package ru.yandex.arenda.element.lk.admin;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Button;

public interface ManagerUsersFilters extends Button {

    String SEARCH_BUTTON = "Найти";

    @Name("ФИО, номер телефона")
    @FindBy(".//input[contains(@id,'input_')]")
    AtlasWebElement queryFilter();
}
