package ru.yandex.arenda.pages;

import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.base.MainHeader;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.common.Label;
import ru.yandex.arenda.element.common.Link;
import ru.yandex.arenda.element.common.Select;
import ru.yandex.arenda.element.lk.admin.StepModal;
import ru.yandex.arenda.element.lk.ownerlk.ToDoRow;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public interface BasePage extends WebPage, Link, Button, Input, Select, Label {

    String MY_FLATS_LINK = "Мои квартиры";
    String PERSONAL_DATA_LINK = "Личные данные";
    String ANKETA_LINK = "Анкета арендатора";
    String DATA_ACCOUNT = "Данные счёта";
    String CARDS_FOR_PAYMENT = "Карты для выплаты";
    String CALL_CENTER_LINK = "Call-центр";
    String FEEDBACK = "Оценка сервиса";

    @Name("h1")
    @FindBy("//h1")
    AtlasWebElement h1();

    @Name("h2")
    @FindBy("//h2")
    AtlasWebElement h2();

    @Name("Заголовок «{{ value }}»")
    @FindBy("//h1[.='{{ value }}']")
    AtlasWebElement h1(@Param("value") String value);

    @Name("root")
    @FindBy("//div[@id='root']")
    AtlasWebElement root();

    @Name("500 заглушка")
    @FindBy(".//div[@class='sticky-content']")
    AtlasWebElement main500();

    @Name("ID запроса")
    @FindBy(".//i[@id='request-id']")
    AtlasWebElement requestId();

    @Name("ID запроса")
    @FindBy(".//div[@id='qr']/*")
    AtlasWebElement qrCode();

    @Name("Главный хедер")
    @FindBy("//header")
    MainHeader header();

    @Name("Мой кабинет")
    @FindBy(".//div[contains(@class,'NavbarMenu__menu')]")
    AtlasWebElement myCabinet();

    @Name("Десктопный попап «Мой кабинет»")
    @FindBy("//div[contains(@class,'NavbarMenuPopup__desktopContent')]")
    Link myCabinetPopupDesktop();

    @Name("Мобильный попап «Мой кабинет»")
    @FindBy("//div[contains(@class,'NavbarMenuPopup__mobileContent')]")
    Link myCabinetPopupMobile();

    @Name("Тост")
    @Retry(polling = 100L)
    @FindBy("//div[contains(@class,'Toast__toast')]")
    AtlasWebElement toast();

    default void successToast() {
        toast().waitUntil(hasText(containsString("Успешно")));
    }

    default void errorToast() {
        toast().waitUntil(hasText(containsString("Ошибка")));
    }

    @Name("Модуль мобилки по заполнению полей")
    @FindBy(".//div[contains(@class,'StepByStepModal__popup') and contains(@class, 'Modal_visible')]")
    StepModal stepModal();

    @Name("Текст заголовка «{{ value }}»")
    @FindBy("//div[contains(@class,'SettingsHeader__container') and contains(.,'{{ value }}')]")
    ToDoRow headerText(@Param("value") String value);
}
