package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SaleListItem extends VertisElement, WithButton {

    @Name("Шапка")
    @FindBy(".//div[@class = 'ListingItemHeader'] | " +
            ".//div[@class = 'ListingAmpItemHeader']")
    VertisElement header();

    @Name("Инфо")
    @FindBy(".//div[contains(@class, 'ListingItemRegular__summary')]")
    VertisElement info();

    @Name("Заголовок")
    @FindBy(".//h3[contains(@class, 'ListingItemHeader__title')] | " +
            ".//a[contains(@class, 'commercial-listing-item__heading')] | " +
            ".//a[contains(@class, 'SearchListingItemExtended__link')]")
    VertisElement title();

    @Name("Предупреждение, что фото из каталога")
    @FindBy(".//div[contains(@class, 'itemBadgeCatalog')]")
    VertisElement photoFromCatalogWarning();

    @Name("Кнопка «Позвонить»")
    @FindBy(".//div[contains(@class,'ListingItemBigPhone__button')] | " +
            ".//button[contains(@class, 'ListingItemRegular__phoneButton')] | " +
            ".//div[contains(@class, 'ListingItemPhoneOrChat__button')] | " +
            ".//div[@class = 'AmpCardGroupItemPhone__button'] |" +
            ".//button[. = 'Позвонить']")
    VertisElement callButton();

    @Name("Кнопка «Написать»")
    @FindBy(".//button[. = 'Написать']")
    VertisElement sendMessage();

    @Name("Кнопка «Смотреть отчёт»")
    @FindBy(".//button[contains(@class, 'vinReportButton')]")
    VertisElement showVinReportButton();

    @Name("Ссылка объявления")
    @FindBy(".//a[contains(@class, 'Link')]")
    VertisElement url();

    @Name("Регион")
    @FindBy(".//div[@class = 'ListingItemBottomRow']")
    VertisElement region();

    @Name("Иконка добавления в избранное")
    @FindBy(".//span[contains(@class, 'like-sale__icon icon_type_like-color-grey')] | " +
            ".//button[contains(@class, 'ButtonFavorite') and .//*[contains(@class, 'ButtonFavorite__icon')]] | " +
            ".//a[contains(@class, 'ButtonFavorite') and .//*[contains(@class, 'ButtonFavorite__icon')]] | " +
            ".//button[contains(@class, 'ButtonFavoriteMobile__button') and .//*[contains(@class, 'ButtonFavoriteMobile__icon')]] | " +
            ".//a[contains(@class, 'ButtonFavoriteMobile__button')]")
    VertisElement addToFavoritesIcon();

    @Name("Иконка удаления из избранного")
    @FindBy(".//span[contains(@class, 'like-sale__icon icon_type_like-color-red')] | " +
            ".//button[contains(@class, 'ButtonFavorite') and .//*[contains(@class, 'ButtonFavorite__icon_active')]] | " +
            ".//button[contains(@class, 'ButtonFavoriteMobile__button') and .//*[contains(@class, 'ButtonFavoriteMobile__icon_active')]]")
    VertisElement deleteFromFavoritesIcon();

    @Name("Иконка ТОП")
    @FindBy(".//div[contains(@class, 'commercial-listing-item__service-icon-top')] | " +
            ".//*[contains(@class, 'IconSvg_vas-icon-top')]")
    VertisElement topIcon();

    @Name("Иконка поднятия в поиске")
    @FindBy(".//*[contains(@class, 'IconSvg_vas-icon-fresh')]")
    VertisElement freshIcon();

    @Name("Выделенная цветом цена")
    @FindBy(".//div[contains(@class, '__price_highlighted')]")
    VertisElement coloredPrice();

    @Name("Иконка сертификации производителя")
    @FindBy("//span[contains(@class, 'ListingItemCertificateIcon')]")
    VertisElement manufacturerCertIcon();

    @Name("Заметка")
    @FindBy("//div[contains(@class,'sale-note_place_listing')] | " +
            "//div[contains(@class, 'ListingItemNote')]")
    VertisElement note();

    @Name("Бейдж «{{ text }}»")
    @FindBy(".//div[contains(@class, 'ListingItemTagsMobile__item') and .= '{{ text }}']")
    VertisElement badge(@Param("text") String text);

    @Name("Список бейджей быстрой продажи")
    @FindBy(".//div[contains(@class, 'Badge_color_lavender')] |" +
            ".//div[contains(@class, 'Badge_color_blueGrayLightExtra')]")
    ElementsCollection<VertisElement> badgesList();

    @Name("Кнопка «N предложений»")
    @FindBy(".//div[contains(@class, 'ListingItemBig__groupInfo')]/a")
    VertisElement showOffersButton();

    @Name("Кнопка «Получить лучшую цену»")
    @FindBy(".//div[contains(@class, 'matchApplicationButton')]/a")
    VertisElement bestOfferButton();

    @Name("Бейдж «Этот автомобиль продан»")
    @FindBy(".//div[contains(@class, 'ListingItemSoldInfo')]")
    VertisElement soldBadge();

    @Name("Галерея")
    @FindBy(".//div[contains(@class, 'ListingItemGallery')]")
    SaleListItemGallery gallery();

    @Name("Статистика звонков")
    @FindBy(".//div[contains(@class, 'callsBadge')]")
    VertisElement callsStats();

    @Name("Кредитная цена")
    @FindBy(".//span[contains(@class, 'CreditPrice')]")
    VertisElement creditPrice();

    @Step("Получаем бейдж с индексом {i}")
    default VertisElement getBadge(int i) {
        return badgesList().should(hasSize(greaterThan(i))).get(i);
    }
}
