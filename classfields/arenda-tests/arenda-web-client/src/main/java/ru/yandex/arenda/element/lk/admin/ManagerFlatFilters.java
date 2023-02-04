package ru.yandex.arenda.element.lk.admin;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.Label;

public interface ManagerFlatFilters extends Button {

    String SEARCH_BUTTON = "Найти";
    String WAITING_FOR_CONFIRMATION_STATUS = "Ожидает\u00a0подтверждения";
    String CONFIRMED_STATUS = "Подтверждено";
    String WORK_IN_PROGRESS_STATUS = "Принята в работу";
    String RENTED_STATUS = "Квартира сдана";
    String DENIED_STATUS = "Отклонено";
    String LOOKING_FOR_TENANT_STATUS = "Ищем\u00a0арендатора";
    String DRAFT = "Черновик";

    @Name("Адрес, ФИО, телефон")
    @FindBy(".//input[@id='query']")
    AtlasWebElement queryFilter();

    @Name("Кнопка выбора статуса заявок")
    @FindBy(".//div[contains(@class,'ManagerSearchFlatsFilters__select')]")
    AtlasWebElement flatFiltersButton();

    @Name("Попап выбора статусов")
    @FindBy("//div[contains(@class,'Popup_visible')]")
    Label flatFiltersPopup();
}
