package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface HorizontalCarousel extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'carousel__header')] | " +
            ".//div[contains(@class, 'CarouselLazyOffers__title')] | " +
            ".//div[contains(@class, 'ListingCarouselNew__title')] | " +
            ".//div[@class = 'ListingCarouselNewestUsed__title'] | " +
            ".//div[contains(@class, 'CarouselUniversal__title')]")
    VertisElement title();

    @Name("Кнопка прокрутки вперед")
    @FindBy(".//li[contains(@class, 'carousel__nav-ctrl_direction_next')] | " +
            ".//i[contains(@class, 'carousel__nav-icon_next')] | " +
            ".//button[@title = 'Вперед'] | " +
            ".//div[contains(@class, 'CarouselUniversal__navButton_next')] | " +
            ".//div[contains(@class, 'NavigationButton_next')]")
    VertisElement nextButton();

    @Name("Кнопка прокрутки назад")
    @FindBy(".//i[contains(@class, 'carousel__nav-icon_prev')] | " +
            ".//li[contains(@class,'carousel__nav-ctrl_direction_prev')] | " +
            ".//button[@title = 'Назад'] | " +
            ".//div[contains(@class, 'CarouselUniversal__navButton_prev')] | " +
            ".//div[contains(@class, 'NavigationButton_prev')]")
    VertisElement prevButton();

    @Name("Список элементов")
    @FindBy(".//li[contains(@class, 'carousel__item')] | " +
            ".//li[contains(@class, 'Carousel__item')] | " +
            ".//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<HorizontalCarouselItem> itemsList();

    @Name("Ссылка на дилера")
    @FindBy(".//div[@class = 'CarouselLazyOffers__title']/a")
    VertisElement dealerUrl();

    @Name("Подсказка")
    @FindBy(".//div[contains(@class, 'specials-sales__how')] | " +
            ".//div[contains(@class, 'InfoPopup')]")
    VertisElement how();

    @Name("Ссылка «Все»")
    @FindBy(".//a[contains(@class, 'carousel__listing-link')] | " +
            ".//a[contains(@class, '__footerLink')] | " +
            ".//a[contains(@class, 'ListingCarouselNewestUsed__footer')]")
    VertisElement allUrl();

    @Step("Получаем элемент карусели с индексом {i}")
    default HorizontalCarouselItem getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}