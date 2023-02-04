package ru.auto.tests.desktop.page.desktopreviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.component.WithOffers;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithReviewsPlusMinusPopup;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.desktopreviews.WithFilters;
import ru.auto.tests.desktop.component.desktopreviews.WithReviewsListing;
import ru.auto.tests.desktop.element.desktopreviews.Summary;
import ru.auto.tests.desktop.element.desktopreviews.VinWidget;
import ru.auto.tests.desktop.page.BasePage;

import java.util.concurrent.TimeUnit;

public interface ReviewsListingPage extends BasePage, WithFilters, WithReviewsListing, WithSelect, WithPager,
        WithOffers, WithReviewsPlusMinusPopup, WithCrossLinksBlock {

    int REVIEWS_LISTING_SIZE = 10;

    @Name("Сводка по всем отзывам о модели")
    @FindBy("//div[@class = 'ReviewsSummaryFull']")
    Summary summary();

    @Name("Виджет «История автомобиля»")
    @FindBy("//div[contains(@class, 'VinWidget')]")
    VinWidget vinWidget();

    enum SortBy {
        DATE("По дате", "updateDate-desc"),
        LIKE("По полезности", "like-desc"),
        COMMENTS("По обсуждаемости", "countComments-desc"),
        RATING_DESC("По рейтингу: сначала положительные", "rating-desc"),
        RATING_ASC("По рейтингу: сначала негативные", "rating-asc"),
        RELEVANCE_DESC("По актуальности", "relevance-exp1-desc");

        private final String name;
        private final String alias;

        SortBy(String stringName, String alias) {
            this.name = stringName;
            this.alias = alias;
        }

        public String getAlias() {
            return alias;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    @Step("Ждём обновления листинга")
    default void waitForListingReload() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }
}