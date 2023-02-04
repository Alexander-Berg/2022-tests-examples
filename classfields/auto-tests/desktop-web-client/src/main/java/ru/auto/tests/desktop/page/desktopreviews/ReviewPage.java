package ru.auto.tests.desktop.page.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.component.WithGallery;
import ru.auto.tests.desktop.component.WithOffers;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithShare;
import ru.auto.tests.desktop.component.WithVideos;
import ru.auto.tests.desktop.component.desktopreviews.WithBreadcrumbs;
import ru.auto.tests.desktop.component.desktopreviews.WithFilters;
import ru.auto.tests.desktop.component.desktopreviews.WithReviewsListing;
import ru.auto.tests.desktop.element.desktopreviews.AuthorBlock;
import ru.auto.tests.desktop.element.desktopreviews.Comments;
import ru.auto.tests.desktop.element.desktopreviews.Cover;
import ru.auto.tests.desktop.element.desktopreviews.ProsAndCons;
import ru.auto.tests.desktop.element.desktopreviews.StickyHeader;
import ru.auto.tests.desktop.page.BasePage;

public interface ReviewPage extends BasePage, WithFilters, WithBreadcrumbs, WithPager, WithOffers, WithVideos,
        WithShare, WithGallery, WithFullScreenGallery, WithReviewsListing {

    @Name("Обложка отзыва")
    @FindBy("//div[@id = 'review-cover']")
    Cover cover();

    @Name("Содержимое отзыва")
    @FindBy("//div[@class = 'Review']")
    VertisElement content();

    @Name("Блок с автором отзыва")
    @FindBy("//div[@class = 'Review__footer']")
    AuthorBlock authorBlock();

    @Name("Блок «Оценка автомобиля»")
    @FindBy("//div[@class = 'ReviewProsAndCons']")
    ProsAndCons prosAndCons();

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

    @Name("Тултип")
    @FindBy("//div[contains(@class, 'Popup_visible') and contains(@class, 'HoveredTooltip')]")
    VertisElement tooltip();

    @Name("Плавающая панель")
    @FindBy("//div[contains(@class, 'ReviewStickyHeader_visible')]")
    StickyHeader stickyHeader();

    @Name("Фрейм с видео")
    @FindBy("//iframe[@class = 'VideoCarousel__videoFrame']")
    VertisElement videoFrame();

    @Name("Электро баннер")
    @FindBy("//div[contains(@class, '_electroBanner')]")
    VertisElement electroBanner();

}
