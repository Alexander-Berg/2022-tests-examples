package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.Footer;
import ru.yandex.realty.mobile.element.Header;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.Modal;
import ru.yandex.realty.mobile.element.main.Menu;
import ru.yandex.realty.mobile.element.main.UserPopup;

/**
 * Created by kopitsa on 16.08.17.
 */
public interface BasePage extends WebPage, Link, Button {

    String LOGIN = "Войти";

    @Name("Вся страница")
    @FindBy("//div[@id='root']")
    AtlasWebElement pageRoot();

    @Name("Хедер")
    @FindBy("//header")
    Header header();

    @Name("Заголовок h1")
    @FindBy("//h1")
    AtlasWebElement h1();

    @Name("Страница «{{ value }}»")
    @FindBy("//h1[contains(.,'{{ value }}')]")
    AtlasWebElement errorPage(@Param("value") String value);

    @Name("Кнопка «Назад»")
    @FindBy("//span[contains(@class,'NavBar__button-back')]")
    AtlasWebElement navButtonBack();

    @Name("Кнопка меню")
    @FindBy("//button[contains(@class,'Header__menu')]")
    AtlasWebElement menuButton();

    @Name("Главное меню")
    @FindBy("//div[contains(@class,'Modal_visible HeaderMenu__modal')]")
    Menu menu();

    @Name("amp. Главное меню")
    @FindBy("//amp-sidebar")
    Menu ampMenu();

    @Name("Попап авторизованного юзера")
    @FindBy("//div[contains(@class, 'User__popup')]")
    UserPopup userPopup();

    @Name("Модалка")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    Modal modal();

    @Name("Футер")
    @FindBy("//footer")
    Footer footer();

    @Name("Нет предложений")
    @FindBy("//div[contains(@class,'SearchNotFound')]")
    AtlasWebElement searchNotFound();

    @Name("СЕО описание")
    @FindBy("//head/meta[@name = 'description']")
    AtlasWebElement seoDescription();

    @Name("Модуль нескольких телефонов")
    @FindBy(".//div[contains(@class,'Modal_visible') and contains(@class, 'FixedModal')]//a")
    ElementsCollection<AtlasWebElement> phones();

    @Name("Попап заявки на ипотеку альфабанка")
    @FindBy(".//div[contains(@class,'AlfaBankMortgageFormScreen__modal')]")
    AtlasWebElement alfaTouchPopup();
}
