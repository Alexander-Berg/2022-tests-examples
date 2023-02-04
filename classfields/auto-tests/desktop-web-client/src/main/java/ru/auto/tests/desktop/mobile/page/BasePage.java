package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAuthPopup;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithHeader;
import ru.auto.tests.desktop.mobile.component.WithPaymentMethodsPopup;
import ru.auto.tests.desktop.mobile.component.WithSubHeader;
import ru.auto.tests.desktop.mobile.component.WithVas;
import ru.auto.tests.desktop.mobile.element.AddOfferNavigateModal;
import ru.auto.tests.desktop.mobile.element.DiscountPopup;
import ru.auto.tests.desktop.mobile.element.Footer;
import ru.auto.tests.desktop.mobile.element.FromWebToAppSplash;
import ru.auto.tests.desktop.mobile.element.Popup;
import ru.auto.tests.desktop.mobile.element.SavedSearchesPopup;
import ru.auto.tests.desktop.mobile.element.Sidebar;
import ru.auto.tests.desktop.mobile.element.WithInput;
import ru.auto.tests.desktop.mobile.element.catalog.Dropdown;
import ru.auto.tests.desktop.mobile.element.chat.Chat;

public interface BasePage extends WebPage, WithNotifier, WithHeader, WithSubHeader, WithVas,
        WithPaymentMethodsPopup, WithButton, WithInput, WithCheckbox, WithAuthPopup {

    @Name("h1")
    @FindBy("//h1")
    VertisElement h1();

    @Name("//body")
    @FindBy("//body")
    VertisElement body();

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'content']")
    VertisElement content();

    @Name("Тэг <title>")
    @FindBy("//title")
    VertisElement titleTag();

    @Name("Мета og:image")
    @FindBy("//meta[@property = 'og:image']")
    VertisElement metaOgImage();

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, 'nav_visible')] | " +
            "//div[contains(@class, 'HeaderNavMenu_visible')]/div[contains(@class, 'nav__content')] | " +
            "//div[contains(@class, 'HeaderNavMenu_visible')] | " +
            "//amp-sidebar")
    Sidebar sidebar();

    @Name("Выпадушка»")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Dropdown dropdown();

    @Name("Поп-ап")
    @FindBy("(//div[contains(@class, 'Modal_visible')])[last()]//div[contains(@class, 'Modal__container')] | " +
            "//div[contains(@class, 'modal_visible')]//div[contains(@class, 'modal__content')] | " +
            "//div[contains(@class, 'Popup_visible')] | " +
            ".//div[@class='AmpShareModal__content'] | " +
            ".//div[@class='AmpHistoryModal__content']")
    Popup popup();

    @Name("Поп-ап сохраненённого поиска")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__container')]")
    SavedSearchesPopup savedSearchesPopup();

    @Name("Футер")
    @FindBy("//footer | " +
            "//div[@class = 'Footer']")
    Footer footer();

    @Name("Чат")
    @FindBy("//div[contains(@class, 'ChatApp_visible')]")
    Chat chat();

    @Name("Фрейм Яндекс.карт")
    @FindBy("//ymaps[contains(@class, 'ymaps-')]")
    VertisElement yandexMap();

    @Name("Таймер скидки")
    @FindBy("//div[@class = 'PromoPopupDiscountTimer']")
    VertisElement discountTimer();

    @Name("Поп-ап скидки")
    @FindBy("//div[contains(@class, 'VasDiscountPopup')]")
    DiscountPopup discountPopup();

    @Name("Жук")
    @FindBy("//div[contains(@class, 'YndxBug')]")
    VertisElement bug();

    @Name("Навигационное меню в журнале")
    @FindBy(".//nav[@class = 'Navigation']")
    VertisElement articleMenu();

    @Name("Каноникл")
    @FindBy("//link[@rel='canonical']")
    VertisElement canonical();

    @Name("Плашка с разработческой веткой")
    @FindBy("//div[@class = 'DevToolsBranch']")
    VertisElement devToolsBranch();

    @Name("Модалка выбора апп/браузер при подаче оффера")
    @FindBy("//div[contains(@class, 'Modal__container')][.//div[@class = 'AddOfferNavigateModal']]")
    AddOfferNavigateModal addOfferNavigateModal();

    @Name("Сплэш «Отредактируйте объявление в приложении»")
    @FindBy("//div[@class = 'FromWebToAppSplashDefault']")
    FromWebToAppSplash fromWebToAppSlash();

}
