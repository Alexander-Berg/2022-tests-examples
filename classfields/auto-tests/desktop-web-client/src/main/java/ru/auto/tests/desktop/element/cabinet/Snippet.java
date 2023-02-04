package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.cabinet.listing.ClassifiedColumn;

import static ru.auto.tests.desktop.utils.Utils.getMatchedString;

public interface Snippet extends VertisElement, WithButton {

    @Name("Объявление")
    @FindBy("./div[contains(@class, 'SalesList__offer')]")
    VertisElement sale();

    @Name("Фото")
    @FindBy(".//div[contains(@class, 'OfferSnippetRoyalPhoto')]")
    PhotoBlock photo();

    @Name("Тайтл")
    @FindBy(".//a[contains(@class, 'OfferSnippetRoyalSpec__title')]")
    VertisElement title();

    @Name("Блок цены")
    @FindBy(".//div[contains(@class, 'OfferSnippetRoyal__col_price')]")
    PriceBlock priceBlock();

    @Name("VIN-код")
    @FindBy(".//div[contains(@class, 'OfferSnippetRoyalSpec__cell_avtokod')]")
    VertisElement vin();

    @Name("Дата размещения")
    @FindBy(".//span[contains(@class, 'OfferSnippetRoyalInfo__days')]")
    VertisElement date();

    @Name("Кнопка «Объявление»")
    @FindBy(".//div[@class='SaleButtonsRoyal']/div[@class='SaleButtonGroup']/div[contains(@class,'SaleMenu__dropdown')]//span[contains(@class, 'SaleButtonRoyal')]")
    VertisElement saleButton();

    @Name("Кнопки с услугами")
    @FindBy(".//div[@class='SaleButtonsRoyal']/div[contains(@class, 'AutoruButtons__container')]")
    ServiceButtons serviceButtons();

    @Name("Строка с причинами блокировки")
    @FindBy("//div[contains(@class, 'OfferSnippetRoyalErrors')]")
    BanPlaceholder banPlaceholder();

    @Name("Кнопка «Добавить города доставки»")
    @FindBy(".//div[contains(@class, 'OfferSnippetRoyalDelivery')]")
    VertisElement addCitiesButton();

    @Name("Столбец классифайда «{{ text }}»")
    @FindBy(".//div[@class='OfferSnippetRoyalClassifieds__item' and .//div[.='{{ text }}']]")
    ClassifiedColumn classifiedColumn(@Param("text") String text);

    @Name("Авито - кнопка услуги «{{ text }}»")
    @FindBy(".//span[contains(@class, 'AvitoButtons') and .//div[contains(@class, 'SaleButtonRoyal__{{ text }}')]]")
    VertisElement avitoServiceButton(@Param("text") String text);

    @Name("Авито - активная кнопка услуги «{{ text }}»")
    @FindBy(".//div[contains(@class, 'SaleButtonRoyal__{{ text }}_active')]")
    VertisElement avitoActiveServiceButton(@Param("text") String text);

    @Name("Доступность «В наличии»/«В пути»")
    @FindBy(".//div[@class='OfferSnippetRoyalSpec__availability']")
    VertisElement availability();

    @Name("Кнопка подключения мультипостинга «Авто.ру»")
    @FindBy(".//span[contains(@class, 'AutoruButtons__button')]")
    VertisElement multipostingAutoruButton();

    @Name("Кнопка подключения мультипостинга «Авито»")
    @FindBy(".//span[contains(@class, 'AvitoButtons__button')]")
    VertisElement multipostingAvitoButton();

    @Name("Кнопка подключения мультипостинга «Дром»")
    @FindBy(".//span[contains(@class, 'DromButtons')]")
    VertisElement multipostingDromButton();

    @Name("Кнопка подключения услуг «Дром»")
    @FindBy(".//div[contains(@class, 'SaleButtonGroup_multiposting')]/a[contains(@class, 'DromButtons')]")
    VertisElement multipostingDromServicesButton();

    @Name("Позиция в поиске")
    @FindBy(".//a[contains(@class, 'OfferSnippetRoyalPositionPercentage')]")
    VertisElement position();

    default String offerId() {
        return getMatchedString(title().getAttribute("href"), "/(\\d+-[\\d\\D]+?)/");
    }
}
