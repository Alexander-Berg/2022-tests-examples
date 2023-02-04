package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Link;

public interface MainLandingPage extends BasePage {

    String CALCULATOR_FOOTER_LINK_TEXT = "Калькулятор оценки";

    @Name("Футер")
    @FindBy("//footer")
    Link footer();

}
