package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.mobile.component.WithBillingPopup;
import ru.auto.tests.desktop.mobile.component.WithGeoPopup;
import ru.auto.tests.desktop.mobile.component.WithMag;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.component.WithReviewsPlusMinusPopup;
import ru.auto.tests.desktop.mobile.element.cardpage.VinReport;
import ru.auto.tests.desktop.mobile.element.gallery.Panorama;
import ru.auto.tests.desktop.mobile.element.garage.Article;
import ru.auto.tests.desktop.mobile.element.garage.CarSelector;
import ru.auto.tests.desktop.mobile.element.garage.CardForm;
import ru.auto.tests.desktop.mobile.element.garage.CardGallery;
import ru.auto.tests.desktop.mobile.element.garage.CardPromos;
import ru.auto.tests.desktop.mobile.element.garage.CardRecalls;
import ru.auto.tests.desktop.mobile.element.garage.CardReviews;
import ru.auto.tests.desktop.mobile.element.garage.InsuranceBlock;
import ru.auto.tests.desktop.mobile.element.garage.TransportTax;

public interface GarageCardPage extends BasePage,
        WithMag,
        WithReviewsPlusMinusPopup,
        WithBillingPopup,
        WithFullScreenGallery,
        WithButton,
        WithGeoPopup,
        WithRadioButton {

    String CHANGE = "Изменить";
    String DELETE = "Удалить";
    String ALL_OFFERS = "Все объявления";
    String ALL_PARAMETERS = "Все параметры";
    String SALE = "Продать";
    String FOR_SALE = "Продаётся";
    String BECOME_CHECKED_OWNER = "Станьте проверенным собственником";

    @Name("Селектор автомобиля")
    @FindBy("//div[contains(@class, 'PageGarageCardMobile__selector')]")
    CarSelector carSelector();

    @Name("Инфо")
    @FindBy("//div[contains(@class, 'GarageCardVehicleInfoMobile')]")
    VertisElement info();

    @Name("Плашка «{{ text }}»")
    @FindBy(".//div[@role = 'gridcell' and contains(., '{{ text }}')]")
    VertisElement grid(@Param("text") String text);

    @Name("Плашка «Журнал»")
    @FindBy(".//div[contains(@role, 'grid') and .//div[contains(@class, 'magazine')]]")
    VertisElement magGrid();

    @Name("Отзывы")
    @FindBy("//div[contains(@id, 'block-all_reviews')]")
    CardReviews reviews();

    @Name("Отзывные кампании")
    @FindBy("//div[contains(@id, 'block-recalls')]")
    CardRecalls recalls();

    @Name("Форма")
    @FindBy("//div[contains(@class, 'GarageFormFieldsSelector')]")
    CardForm form();

    @Name("Отчёт о проверке по VIN")
    @FindBy("//div[contains(@class, 'CardVinReport')]")
    VinReport vinReport();

    @Name("Панорама")
    @FindBy("//div[contains(@class, 'OfferPanorama')]")
    Panorama panorama();

    @Name("Страховки")
    @FindBy("//div[contains(@class, 'GarageCardInsuranceList')]")
    InsuranceBlock insurances();

    @Name("Попап подтверждения удаления страховки")
    @FindBy("//div[contains(@class, 'GarageCardInsuranceFormModal__confirmation')]")
    Popup insuranceDeleteConfirmPopup();

    @Name("Список офферов из карусели")
    @FindBy("//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<VertisElement> offerCarousel();

    @Name("Список статей и отзывов")
    @FindBy("//div[@class = 'GarageCardArticlesAndReviewsItem']")
    ElementsCollection<Article> articlesAndReviews();

    @Name("Галерея")
    @FindBy("//div[contains(@class, 'EditableImageGallery_mobile')]")
    CardGallery gallery();

    @Name("Бейдж «{{ text }}»")
    @FindBy("//span[contains(@class, 'GarageBadge') and contains(., '{{ text }}')] |" +
            "//div[contains(@class, 'CardMobile__label') and contains(., '{{ text }}')]")
    VertisElement badge(@Param("text") String text);

    @Name("Транспортный налог")
    @FindBy("//div[contains(@class, 'GarageCardTaxMobile')]")
    TransportTax transportTax();

    @Name("Блок «Акции и скидки»")
    @FindBy("//div[@id = 'block-regular-promos']")
    CardPromos promos();

}
