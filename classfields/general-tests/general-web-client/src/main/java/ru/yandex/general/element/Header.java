package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Header extends VertisElement, Link {

    String MY_OFFERS = "Мои объявления";
    String FAVORITE = "Избранное";
    String HELP = "Помощь";
    String FOR_SHOPS = "Магазинам";
    String LOGIN = "Войти";
    String CHATS = "Чаты";


    @Name("Имя юзера")
    @FindBy(".//span[contains(@class, 'UserInfo__slicedText')]")
    VertisElement userName();

    @Name("Аватар")
    @FindBy(".//img[contains(@class, 'Avatar')]")
    VertisElement avatar();

}
