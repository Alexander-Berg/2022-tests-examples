package ru.auto.tests.desktop.mobile.page.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithReviewsPlusMinusPopup;
import ru.auto.tests.desktop.mobile.component.WithSortBar;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithFilters;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithReviewsListing;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithSales;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Summary;
import ru.auto.tests.desktop.page.BasePage;

public interface ReviewsListingPage extends BasePage, WithReviewsListing, WithSortBar,
        WithSales, WithFilters, WithReviewsPlusMinusPopup {

    int REVIEWS_LISTING_SIZE = 10;

    @Name("Сводка по всем отзывам о модели")
    @FindBy("//div[@class = 'ReviewSummaryRatings']")
    Summary summary();

    @Name("Кнопка «Предыдущие»")
    @FindBy("//div[contains(@class, 'PageReviewsListing__more_prev')]")
    VertisElement prevPageButton();

    @Name("Ссылка «Все отзывы»")
    @FindBy("//a[contains(@class, 'PageReviewsListing__parentListingLink')]")
    VertisElement allReviwesUrl();

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
}
