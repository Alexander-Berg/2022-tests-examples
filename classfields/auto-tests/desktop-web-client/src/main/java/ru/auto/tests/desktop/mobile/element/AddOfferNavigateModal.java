package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AddOfferNavigateModal extends VertisElement {

    String SAFARI = "Safari";
    String AUTO_RU = "Auto.ru";
    String CHROME = "Chrome";

    @Name("Приложение «{{ text }}»")
    @FindBy(".//div[@class = 'AddOfferNavigateModal__item'][contains(., '{{ text }}')]")
    AddOfferNavigateModalItem item(@Param("text") String Text);

    @Name("Иконка закрытия")
    @FindBy(".//*[contains(@class, '_closer_icon')]")
    VertisElement closeIcon();

}
