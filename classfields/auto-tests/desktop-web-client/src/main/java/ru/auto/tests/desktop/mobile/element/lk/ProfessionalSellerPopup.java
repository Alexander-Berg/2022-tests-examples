package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface ProfessionalSellerPopup extends VertisElement, WithButton {

    String GET_STATUS = "Получить статус";
    String LATER = "Позже";
    String PROFILE = "профиле";
    String VIEW_OFFERS_PAGE = "Посмотреть страницу объявлений";

    @Name("Иконка закрытия")
    @FindBy(".//*[contains(@class, 'closer_icon')]")
    VertisElement close();

}
