package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithMag;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithReviews;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.card.gallery.Panorama;
import ru.auto.tests.desktop.element.garage.Article;
import ru.auto.tests.desktop.element.garage.CardForm;
import ru.auto.tests.desktop.element.garage.CardGallery;
import ru.auto.tests.desktop.element.garage.CardLeftColumn;
import ru.auto.tests.desktop.element.garage.CardPromos;
import ru.auto.tests.desktop.element.garage.CardRecalls;
import ru.auto.tests.desktop.element.garage.CardReviewsPromo;
import ru.auto.tests.desktop.element.garage.CardVinReport;
import ru.auto.tests.desktop.element.garage.InsuranceBlock;
import ru.auto.tests.desktop.element.garage.TransportTax;

public interface GarageCardPage extends BasePage, WithReviews, WithMag, WithFullScreenGallery, WithGeoSuggest,
        WithRadioButton {

    String ALL_OFFERS = "Все объявления";
    String ALL_PARAMETERS = "Все параметры";
    String MY_EX = "Моя бывшая";
    String ARTICLES = "Статьи";
    String REVIEWS = "Отзывы";
    String WATCH_MORE = "Смотреть ещё";
    String PASS_VERIFICATION = "Пройти проверку";

    @Name("Левая колонка")
    @FindBy("//div[contains(@class, 'Card__columnLeft')]")
    CardLeftColumn leftColumn();

    @Name("Промо отзывов")
    @FindBy("//div[contains(@class, 'GarageCardAddReviewPromo')]")
    CardReviewsPromo reviewsPromo();

    @Name("Отзывные кампании")
    @FindBy("//div[contains(@id, 'block-recalls')]")
    CardRecalls recalls();

    @Name("Отчёт о проверке по VIN")
    @FindBy("//div[contains(@class, 'CardVinReport')]")
    CardVinReport vinReport();

    @Name("Форма")
    @FindBy("//div[contains(@class, 'GarageFormFieldsSelector')]")
    CardForm form();

    @Name("Панорама")
    @FindBy("//div[contains(@class, 'PanoramaExterior')]")
    Panorama panorama();

    @Name("Страховки")
    @FindBy("//div[contains(@class, 'GarageCardInsuranceList')]")
    InsuranceBlock insurances();

    @Name("Попап подтверждения удаления страховки")
    @FindBy("//div[contains(@class, 'GarageCardInsuranceFormModal__confirmation')]")
    Popup insuranceDeleteConfirmPopup();

    @Name("Список офферов из карусели")
    @FindBy("//li[contains(@class, 'CardCarousel__item')]")
    ElementsCollection<VertisElement> offerCarousel();

    @Name("Список статей и отзывов")
    @FindBy("//div[@class = 'GarageCardArticlesAndReviewsItem']")
    ElementsCollection<Article> articlesAndReviews();

    @Name("Текст элемента информации о «{{ title }}»")
    @FindBy("//div[contains(@class, 'VehicleInfoDesktop__item')][.//div[contains(@class, '_itemTitle') and " +
            ".= '{{ title }}']]//div[contains(@class, '_itemText')]")
    VertisElement vehicleInfoText(@Param("title") String title);

    @Name("Кнопка «Сохранить»")
    @FindBy("//div[contains(@class, 'GarageSubmitButton')]/button")
    VertisElement submitButton();

    @Name("Галерея")
    @FindBy("//div[contains(@class, '_galleryWrapper')]")
    CardGallery gallery();

    @Name("Транспортный налог")
    @FindBy("//div[@class = 'GarageCardTaxDesktop']")
    TransportTax transportTax();

    @Name("Блок «Акции и скидки»")
    @FindBy("//div[@class = 'GarageCardPromos']")
    CardPromos promos();

}
