package ru.auto.tests.desktop.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAutoruOnlyBadge;
import ru.auto.tests.desktop.component.WithBadge;
import ru.auto.tests.desktop.element.Price;
import ru.auto.tests.desktop.element.card.NoteBar;
import ru.auto.tests.desktop.element.card.header.ToolBar;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SalesListItem extends VertisElement,
        WithBadge,
        WithAutoruOnlyBadge {

    String FROM_OWNER = "От собственника";
    String HOW_PASS_VERIFICATION = "Как пройти проверку";
    String CIRCULAR_VIEW_CAMERA = "Камера кругового обзора";
    String DISCOUNTS = "Скидки";
    String PHOTO_FROM_CATALOG = "Фото из каталога";

    String FROM_OWNER_BADGE_POPUP = "Продаёт собственник\nМы получили копии СТС и водительского удостоверения от " +
            "продавца автомобиля\nКак пройти проверку";
    String OWNER_HOW_PASS_VERIFICATION_POPUP = "Покупатели ценят автомобили от собственников\n" +
            "Расскажите всем, что вы один из них — получите бейджик «Продаёт собственник».\n\n" +
            "Скачайте приложение Авто.ру и ищите в сообщениях чат с поддержкой. Разбудите бота любым приветствием, " +
            "а потом выберите команду «Хочу стать проверенным собственником».\nAppStore\nGoogle Play\nAppGallery\n" +
            "Наведите камеру смартфона на QR код, чтобы скачать или открыть приложение";

    @Name("Заметка")
    @FindBy(".//span[contains(@class, 'ListingItem__note')] | " +
            ".//div[contains(@class, 'ListingItem__note')]")
    NoteBar noteBar();

    @Name("Тулбар с действиями над объявлением")
    @FindBy(".//td[contains(@class, 'listing__cell_type_actions')] | " +
            ".//div[contains(@class, 'ListingItem-module__columnCellActions')] | " +
            ".//div[contains(@class, 'ListingItemActions')]")
    ToolBar toolBar();

    @Name("Стикеры")
    @FindBy(".//div[contains(@class, 'ListingItemTags')]")
    VertisElement badges();

    @Name("Ссылка на объявление")
    @FindBy(".//a[contains(@class, 'ListingItemTitle-module__link')] | " +
            ".//a[contains(@class, 'listing-item__link')] | " +
            ".//a[contains(@class, 'ListingItemTitle__link')]")
    VertisElement nameLink();

    @Name("Ссылка на дилера")
    @FindBy(".//a[contains(@class,'__dealer') or contains(@class, 'salonName')]")
    VertisElement dealerUrl();

    @Name("Ссылка «На Авто.ру с N года»")
    @FindBy(".//a[contains(@class, 'extendedLabelsLink') and contains(., 'На Авто.ру')]")
    VertisElement autoruYearUrl();

    @Name("Ссылка «N т.с. в наличии»")
    @FindBy(".//a[contains(@class, 'extendedLabelsLink') and contains(., 'в наличии')]")
    VertisElement inStockUrl();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//div[contains(@class, '__show-phone')] | " +
            ".//button[contains(@class, 'ListingItem-module__extendedPhone')] | " +
            ".//button[contains(@class, 'ListingItem-module__showPhoneButton')] | " +
            ".//button[.//span[.= 'Показать телефон']] | " +
            ".//div[contains(@class, '__phoneButton')]/button")
    VertisElement showPhonesButton();

    @Name("Кнопка «Написать»")
    @FindBy(".//div[contains(@class, '_chatButton')]")
    VertisElement sendMessage();

    @Name("Иконка сертификации")
    @FindBy(".//span[contains(@class, 'ListingItemCertificateIcon')]")
    VertisElement certificationIcon();

    @Name("Цена на оффер")
    @FindBy(".//div[contains(@class,'__price') and not(contains(@class, 'old'))]")
    Price price();

    @Name("Иконка ТОП")
    @FindBy(".//span[contains(@class, 'icon_type_top dropdown')] | " +
            ".//*[contains(@class, '_vas-icon-top')]")
    VertisElement topIcon();

    @Name("Иконка поднятия в поиске")
    @FindBy(".//span[contains(@class, 'icon_type_top-search dropdown')] | " +
            ".//*[contains(@class, '_vas-icon-fresh')]")
    VertisElement freshIcon();

    @Name("Фото")
    @FindBy(".//div[contains(@class, 'ListingItem__thumb')]")
    VertisElement photo();

    @Name("Переключалка Brazzers")
    @FindBy(".//div[contains(@class, 'Brazzers__button_visible')]")
    VertisElement brazzersSwitcher();

    @Name("Предупреждение, что фото из каталога")
    @FindBy(".//div[contains(@class, 'OfferThumb__fakeImageLabel')]")
    VertisElement photoFromCatalogWarning();

    @Name("Полный текст предупреждения, что фото из каталога")
    @FindBy("//div[contains(@class, 'OfferThumb__fakeImageTooltip')]")
    VertisElement photoFromCatalogWarningTooltip();

    @Name("Список фотографий")
    @FindBy(".//div[contains(@class, 'Brazzers__page')]")
    ElementsCollection<VertisElement> photosList();

    @Name("Кредитная цена")
    @FindBy(".//span[contains(@class, 'CreditPrice')]")
    VertisElement creditPrice();

    @Name("Кнопка «Получить лучшую цену»")
    @FindBy(".//button[contains(@class, 'matchApplicationButton')]")
    VertisElement bestOfferButton();

    @Name("Статус бронирования «{{ text }}»")
    @FindBy(".//div[contains(@class, 'BookingOfferLabel') and .= '{{ text }}']")
    VertisElement bookingStatus(@Param("text") String text);

    @Name("Стикер «Этот автомобиль продан»")
    @FindBy(".//div[contains(@class, 'ListingItemSoldInfo')]")
    VertisElement soldBadge();

    @Name("Стикеры на группе")
    @FindBy(".//div[contains(@class, 'ListingItemGroup__badges')]")
    VertisElement groupBadges();

    @Name("Ссылка на профиль перекупа")
    @FindBy(".//a[contains(@class, '_resellerLink')]")
    VertisElement resellerLink();

}
