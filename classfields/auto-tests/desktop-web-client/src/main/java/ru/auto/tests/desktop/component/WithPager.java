package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Pager;

public interface WithPager {

    @Name("Пагинатор")
    @FindBy("//div[contains(@class,'pager-listing')] | " +
            "//div[@class = 'ReviewComments__pagination'] | " +
            "//div[@class = 'PageReviewsListing__pagination'] | " +
            "//div[@class = 'PageResellerPublic_pagination'] | " +
            "//div[contains(@class, 'MyWalletHistory__pagination')] | " +
            "//div[contains(@class, 'pager-catalog')] |" +
            "//div[contains(@class, 'ListingCarsPagination')] | " +
            "//div[contains(@class, 'ListingPagination__page')] | " +
            "//div[@class = 'ListingPagination'] | " +
            "//div[contains(@class, 'ListingPagination ')]")
    Pager pager();
}
