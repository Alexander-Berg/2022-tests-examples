package ru.auto.tests.desktop.mobile.page.mobilereviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.desktopreviews.WithReviewsListing;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithGallery;
import ru.auto.tests.desktop.mobile.component.WithPager;
import ru.auto.tests.desktop.mobile.component.WithReviewsPlusMinusPopup;
import ru.auto.tests.desktop.mobile.component.WithShare;
import ru.auto.tests.desktop.mobile.component.WithVideos;
import ru.auto.tests.desktop.mobile.component.mobilereviews.ItemsSliderBlock;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithFilters;
import ru.auto.tests.desktop.mobile.element.WithInput;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Comments;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Rate;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Ratings;
import ru.auto.tests.desktop.mobile.page.BasePage;

public interface ReviewPage extends BasePage, WithFilters, WithPager, WithVideos,
        WithGallery, WithReviewsListing, WithShare, WithInput, WithReviewsPlusMinusPopup, WithButton {

    @Name("Содержимое отзыва")
    @FindBy("//div[@class = 'ReviewContent'] | " +
            "//div[@class = 'AmpReview']")
    VertisElement reviewContent();

    @Name("Блок «Оценка автора»")
    @FindBy("//div[contains(@class, 'ReviewRatings')]")
    VertisElement authorBlock();

    @Name("Блок «Отзыв полезен?»")
    @FindBy("//div[contains(@class, 'ReviewRate')]")
    Rate rate();

    @Name("Блок «Отзывы владельцев»")
    @FindBy("//div[@class = 'ReviewSummaryRatings']")
    Ratings ratings();

    @Name("Кнопка «Подтвердить»")
    @FindBy("//button[contains(@class, 'LazyPhoneAuth__button')]")
    VertisElement confirmButton();

    @Name("Поле «Код из SMS»")
    @FindBy("//input[@name = 'code']")
    VertisElement codeInput();

    @Name("Комментарии")
    @FindBy("//div[@id = 'reviewComments']")
    Comments comments();

    @Name("Баннер Дзена")
    @FindBy("//a[contains(@class, 'ReviewZenPromo__link')]")
    VertisElement zenBanner();

    @Name("Счётчик просмотров")
    @FindBy(".//div[@class = 'Review__firstline']")
    VertisElement counter();

    @Name("Ссылка «Все отзывы о ...»")
    @FindBy("//a[contains(@class, 'PageReviewCard__listingLink')]")
    VertisElement allReviewsUrl();

    @Name("Ссылка на профиль пользователя")
    @FindBy("//a[contains(@class, 'AmpReview__userInfoClick')]")
    VertisElement authorProfileLink();

    @Name("Блок с объявлениями")
    @FindBy("//div[contains(@class, 'SaleCarousel')]")
    ItemsSliderBlock offersBlock();

    @Name("Блок с видео")
    @FindBy("//div[contains(@class, 'VideoCarousel')]")
    ItemsSliderBlock videoBlock();

    @Name("Электро баннер")
    @FindBy("//div[@class = 'ElectroBannerTouch']")
    VertisElement electroBanner();

    @Step("Получаем счётчик просмотров")
    default int getCounter() {
        String text = counter().getText();
        return Integer.parseInt(text.substring(text.lastIndexOf("\n") + 1));
    }
}
