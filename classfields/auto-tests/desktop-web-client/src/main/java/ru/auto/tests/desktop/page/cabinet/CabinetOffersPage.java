package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.cabinet.WithCalendar;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.cabinet.AuctionPopup;
import ru.auto.tests.desktop.element.cabinet.AutostrategyConfigurator;
import ru.auto.tests.desktop.element.cabinet.DeliveryPopup;
import ru.auto.tests.desktop.element.cabinet.EnrollOnCheckPopup;
import ru.auto.tests.desktop.element.cabinet.PopupEditing;
import ru.auto.tests.desktop.element.cabinet.PriceDiscountPopup;
import ru.auto.tests.desktop.element.cabinet.QuickSaleStickers;
import ru.auto.tests.desktop.element.cabinet.RemovePopup;
import ru.auto.tests.desktop.element.cabinet.SalesFiltersBlock;
import ru.auto.tests.desktop.element.cabinet.ServiceButtons;
import ru.auto.tests.desktop.element.cabinet.ServiceConfirmPopup;
import ru.auto.tests.desktop.element.cabinet.SetAutocapturePopup;
import ru.auto.tests.desktop.element.cabinet.Snippet;
import ru.auto.tests.desktop.element.cabinet.SortBlock;
import ru.auto.tests.desktop.element.cabinet.UpdatesPerDayPopup;
import ru.auto.tests.desktop.element.cabinet.WithMenuPopup;
import ru.auto.tests.desktop.element.cabinet.header.Header;
import ru.auto.tests.desktop.element.cabinet.listing.AvitoPopup;
import ru.auto.tests.desktop.element.cabinet.listing.FavoriteDiscountsCurtain;
import ru.auto.tests.desktop.element.cabinet.offerstat.OfferStat;
import ru.auto.tests.desktop.element.cabinet.sidebar.Sidebar;
import ru.auto.tests.desktop.element.chat.Chat;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetOffersPage extends BasePage, WithNotifier, WithCalendar, WithSelect, WithPager, WithMenuPopup {

    String ARCHIVE = "Архив";

    @Name("Шапка")
    @FindBy("//div[contains(@class, 'Header__container')]")
    Header header();

    @Name("Страница подробной статистики по объявлению")
    @FindBy("//div[@class = 'Offer']")
    OfferStat offerstat();

    @Name("Чат")
    @FindBy("//div[@class = 'ChatApp']")
    Chat chat();

    @Name("Боковое меню")
    @FindBy("//div[contains(@class, 'Sidebar')]")
    Sidebar sidebar();

    @Name("Заглушка «Объявления не найдены»")
    @FindBy("//div[contains(@class, 'ListingEmpty')]")
    VertisElement stub();

    @Name("Фильтры")
    @FindBy("//div[@class = 'Sales__filters']")
    SalesFiltersBlock salesFiltersBlock();

    @Name("Шапка сортировок")
    @FindBy("//div[contains(@class, 'Listing__header__inner')]")
    SortBlock sort();

    @Name("Групповые действия с объявлениями")
    @FindBy("//div[contains(@class, 'SaleButtonsRoyal_group')]//button[contains(@class, 'SaleMenuGroup__button')]")
    VertisElement groupActionsButton();

    @Name("Групповые услуги")
    @FindBy("//div[contains(@class, 'SaleButtonsRoyal_group')]")
    ServiceButtons groupServiceButtons();

    @Name("Объявления")
    @FindBy("//div[@class = 'Listing__item'] | " +
            "//div[@class = 'Listing__item Listing__item_multiposting']")
    ElementsCollection<Snippet> snippets();

    @Name("Конфигуратор автостратегии")
    @FindBy("//div[contains(@class, 'Modal_visible') " +
            "and contains(., 'Конфигуратор автостратегии')]//div[contains(@class, 'Modal__content')]")
    AutostrategyConfigurator autostrategyConfigurator();

    @Name("Поп-ап «Стикеры быстрой продажи»")
    @FindBy("//div[contains(@class, 'ServicePopup')]")
    QuickSaleStickers quickSaleStickers();

    @Name("Активный поп-ап без фона")
    @FindBy("//div[contains(@class, 'Modal_visible')] //div[@class='Modal__content']")
    QuickSaleStickers activePopupWithoutBackground();

    @Name("Поп-ап с вариантами редактирования")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    PopupEditing popupEditing();

    @Name("Активный поп-ап")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[@class = 'ServicePopup-module__popup'] | " +
            "//div[contains(@class, 'Popup_visible')]")
    VertisElement activePopup();

    @Name("Поп-ап с меню")
    @FindBy("//div[contains(@class, 'Popup_visible') and .//div[contains(@class, 'MenuItem')]]")
    WithMenuPopup withMenuPopup();

    @Name("Поп-ап «Настроить автоприменение»")
    @FindBy("//div[@class='ServicePopupSchedule']")
    SetAutocapturePopup autocapturePopup();

    @Name("Поп-ап «Обновления в день»")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    UpdatesPerDayPopup updatesPerDayPopup();

    @Name("Поп-ап «Запись на проверку Авто.ру»")
    @FindBy("//div[@class = 'certification__modal']")
    EnrollOnCheckPopup enrollOnCheckPopup();

    @Name("Поп-ап с предупрждением")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__content')]")
    VertisElement warningPopup();

    @Name("Поп-ап удаления офера")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__content')]")
    RemovePopup removePopup();

    @Name("Поп-ап подтверждения применения услуги")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    ServiceConfirmPopup serviceConfirmPopup();

    @Name("Поп-ап доставки")
    @FindBy("//div[contains(@class, 'DeliverySettings__modal')]//div[contains(@class, 'Modal__content')]")
    DeliveryPopup deliveryPopup();

    @Name("Саджест {{ value }}")
    @FindBy("//div[contains(@class,'Header__addOfferMenu')]//div[contains(@class,'HeaderMenuGroup') and contains(., '{{ value }}')]")
    VertisElement suggestCategoryItem(@Param("value") String value);

    @Name("Поп-ап Авито")
    @FindBy("//div[contains(@class, 'ServicesAvitoTooltipContent')]")
    AvitoPopup avitoPopup();

    @Name("Поп-ап редактирования цены/скидок")
    @FindBy("//div[contains(@class, 'SalePrice__editPopup')]")
    PriceDiscountPopup priceDiscountPopup();

    @Name("Поп-ап с описанием цены/скидок")
    @FindBy("//div[contains(@class, 'SalePricePopupContent')]")
    Popup priceDescriptionPopup();

    @Name("Шторка отправки сообщения о скидке")
    @FindBy("//div[contains(@class, 'FavoriteDiscountsCurtain')]")
    FavoriteDiscountsCurtain favoriteDiscountsCurtain();

    @Name("Элемент с промо панорам")
    @FindBy("//a[contains(@class, 'PanoramaPromoAddInAppCabinet')]")
    VertisElement panoramaPromo();

    @Name("Попап онбординга")
    @FindBy("//div[contains(@class, 'OnboardingPopup__popup')]")
    OnboardingPopup onboardingPopup();

    @Name("Активный таб")
    @FindBy("//div[contains(@class, 'TabsItem_active')]")
    VertisElement activeTab();

    @Name("Попап аукциона")
    @FindBy("//div[@class = 'AuctionUsedPopupContent']")
    AuctionPopup auctionPopup();

    @Name("Модал аукциона")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'AuctionUsedModalContent')]")
    Popup auctionModal();

    @Name("Тултип интереса")
    @FindBy("//div[contains(@class , 'Popup_visible') and contains(@class , '_interestTooltip')]")
    VertisElement interestTooltip();

    @Name("Тултип позиции в поиске")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'PositionPercentageTooltip')]")
    VertisElement positionTooltip();

    default Snippet snippet(int i) {
        return snippets().should(hasSize(greaterThan(i))).get(i);
    }

    default Snippet snippet(String id) {
        return snippets().should(hasSize(greaterThan(0))) // TODO: 24.07.18 проверить можно ли удалить эту проверку
                .filter(r -> r.offerId().equals(id)).should(hasSize(greaterThan(0))).get(0);
    }
}
