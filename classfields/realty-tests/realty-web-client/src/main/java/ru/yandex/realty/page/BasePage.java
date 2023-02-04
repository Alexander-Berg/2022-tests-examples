package ru.yandex.realty.page;

import io.qameta.atlas.core.api.Retry;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.AlfaBankMortgage;
import ru.yandex.realty.element.AlfaModal;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.DomikPopup;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.base.Footer;
import ru.yandex.realty.element.base.GeoSelectorPopup.GeoSelectorPopup;
import ru.yandex.realty.element.base.GeoSelectorPopup.RegionSelectorPopup;
import ru.yandex.realty.element.base.HeaderMain;
import ru.yandex.realty.element.base.HeaderUnder;
import ru.yandex.realty.element.popups.RefillWalletPopup;
import ru.yandex.realty.element.saleads.Popups;

/**
 * Created by vicdev on 21.04.17.
 */
public interface BasePage extends WebPage, DomikPopup, Popups, Button, AlfaModal,
        AlfaBankMortgage {

    String PAGE_NOT_FOUND = "Нет такой страницы";
    String SERVICE_UNAVAILABLE = "Произошла ошибка";
    String MY_CABINET = "Кабинет";
    String FAVORITES = "Избранное";
    String REPORTS = "Отчёты";
    String SUBSCRIPTIONS = "Подписки";
    String COMPARISON = "Сравнение";
    String AD_IN_NEWBUILDINGS = "Реклама в новостройках";
    String FOR_PROFESSIONAL = "Профессионалам";
    String PROFPOISK_LINK = "Профпоиск";
    String JOURNAL_LINK = "Журнал недвижимости";

    @Name("Главный хедер")
    @FindBy("//div[@class= 'NavBar']")
    HeaderMain headerMain();

    @Name("Подхедер")
    @FindBy("//div[contains(@class,'MainMenu__wrapper')]/nav")
    HeaderUnder headerUnder();

    @Name("Вся страница")
    @FindBy("//body")
    AtlasWebElement pageBody();

    @Name("Вся страница")
    @FindBy("//div[@class='PageContent']")
    AtlasWebElement pageContent();

    @Name("Попап с выбором локации")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    GeoSelectorPopup geoSelectorPopup();

    @Name("Попап с выбором региона")
    @FindBy("//div[@class='RegionSelector']")
    RegionSelectorPopup regionSelectorPopup();

    @Name("Футер")
    @FindBy("//div[@class='MegaFooter']")
    Footer footer();

    @Name("Паранжа")
    @FindBy("//div[contains(@class, 'paranja_state_open')]")
    AtlasWebElement paranja();

    @Name("Попап с подтверждением региона ")
    @FindBy(".//div[contains(@class, 'Popup_visible')]")
    Button geoApplyPopup();

    @Name("Попап «Пополнить кошелёк»")
    @FindBy(".//div[contains(@class, 'Modal__content') and contains(., 'Пополнить кошелёк')]")
    RefillWalletPopup refillWalletPopup();

    @Name("Открытый попап")
    @FindBy(".//div[contains(@class, 'Popup_visible')]")
    Link openedPopup();

    @Name("Попап юзера")
    @FindBy(".//div[contains(@class, 'UserNewPopup')]")
    Link userNewPopup();

    @Name("Открытый модуль")
    @FindBy(".//div[contains(@class, 'Modal_visible')]")
    AtlasWebElement modalVisible();

    @Retry(timeout = 1000)
    @Name("Страница «{{ value }}»")
    @FindBy("//h1[contains(.,'{{ value }}')]")
    AtlasWebElement errorPage(@Param("value") String value);

    @Name("Полоска загрузки")
    @FindBy("//div[contains(@class,'LoaderBar')]")
    @Retry(polling = 100L)
    AtlasWebElement loader();

    @Name("СЕО описание")
    @FindBy("//head/meta[@name = 'description']")
    AtlasWebElement pageDescription();

    @Name("Заголовок h1")
    @FindBy("//h1")
    AtlasWebElement pageH1();

    @Name("Тайтл")
    @FindBy("//head/title")
    AtlasWebElement pageTitle();
}
