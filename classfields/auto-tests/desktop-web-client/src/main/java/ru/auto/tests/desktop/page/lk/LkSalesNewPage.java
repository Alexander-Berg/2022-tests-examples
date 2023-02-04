package ru.auto.tests.desktop.page.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivatePopup;
import ru.auto.tests.desktop.component.WithAutoProlongDiscountBanner;
import ru.auto.tests.desktop.component.WithCheckWalletBanner;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.WithReviewsPromo;
import ru.auto.tests.desktop.component.WithSoldPopup;
import ru.auto.tests.desktop.element.lk.BannedMessage;
import ru.auto.tests.desktop.element.lk.ChartPopup;
import ru.auto.tests.desktop.element.lk.PricePopup;
import ru.auto.tests.desktop.element.lk.RadioButtonGroup;
import ru.auto.tests.desktop.element.lk.SalesListNewItem;
import ru.auto.tests.desktop.element.lk.SalesStub;
import ru.auto.tests.desktop.element.lk.Sidebar;
import ru.auto.tests.desktop.element.lk.ThreeDotsMenu;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkSalesNewPage extends BasePage, WithSoldPopup, WithReviewsPromo, WithActivatePopup,
        WithCheckWalletBanner, WithAutoProlongDiscountBanner, WithContactsPopup {

    String OFFER_DELETED = "Объявление удалено";

    @Name("Список объявлений пользователя")
    @FindBy("//div[@class = 'SalesItemNewDesign']")
    ElementsCollection<SalesListNewItem> salesList();

    @Name("Сообщение о забаненом пользователе")
    @FindBy("//div[contains(@class, 'BanMessage')]")
    BannedMessage bannedMessage();

    @Name("Вкладка «{{ text }}»")
    @FindBy(".//a[contains(@class, 'Tabs__link') and . = '{{ text }}']")
    VertisElement tab(@Param("text") String text);

    @Name("Ссылка из подшапки «{{ text }}»")
    @FindBy(".//a[contains(@class, 'ServiceNavigation__link') and . = '{{ text }}']")
    VertisElement navigationLink(@Param("text") String text);

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'SalesEmptyListLk_newDesign')]")
    SalesStub stub();

    @Name("Кнопка «Показать ещё»")
    @FindBy(".//div[contains(@class, 'SalesPager')]")
    VertisElement loadMoreButton();

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, 'UserOffersWithSidebar__aside')]")
    Sidebar sidebar();

    @Name("Госуслуги")
    @FindBy("//div[contains(@class, 'GosUslugiBenefit_newDesign')]")
    VertisElement gosuslugi();

    @Name("Иконка закрытия баннера «Вместе мы справимся!»")
    @FindBy(".//*[contains(@class, 'CovidResellerBanner__closer')]")
    VertisElement covidResellerBannerCloseIcon();

    @Name("Баннер AdFox")
    @FindBy(".//a[contains(@href, 'ads.adfox.ru')]")
    VertisElement adFoxBanner();

    @Name("Попап с дополнительными опциями объявления")
    @FindBy("//div[@class='SalesItemNewDesignControls__moreMenu']")
    ThreeDotsMenu threeDotsMenu();

    @Name("Персональный ассистент продажи")
    @FindBy("//div[contains(@class, 'SaleAssistant')]")
    VertisElement personalAssistant();

    @Name("Вкладки категорий")
    @FindBy("//span[contains(@class, 'RadioGroup_type_button')]")
    RadioButtonGroup radioButtonGroup();

    @Name("Поп-ап над столбцами графика")
    @FindBy("//div[contains(@class, 'Popup_visible') and contains(@class, 'SalesChart__tooltip')]")
    ChartPopup chartPopup();

    @Name("Попап редактирования цены")
    @FindBy(".//div[contains(@class,'SalesItemPriceNewDesign__popupContent')]")
    PricePopup pricePopup();

    @Name("Попап услуги")
    @FindBy(".//div[@class = 'SalesVasItemPopup']")
    VertisElement vasItemPopup();

    @Step("Получаем объявление с индексом «{i}»")
    default SalesListNewItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

}
