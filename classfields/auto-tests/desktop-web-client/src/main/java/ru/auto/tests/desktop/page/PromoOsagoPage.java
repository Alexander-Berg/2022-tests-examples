package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PromoOsagoPage extends PromoPage {

    String TAKE_CREDIT = "Сначала возьму кредит →";

    @Name("Фрейм Sravni.ru")
    @FindBy("//iframe")
    VertisElement sravniRuIframe();
}