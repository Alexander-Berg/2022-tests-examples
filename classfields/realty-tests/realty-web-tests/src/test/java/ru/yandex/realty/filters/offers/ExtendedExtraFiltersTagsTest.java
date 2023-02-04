package ru.yandex.realty.filters.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedExtraFiltersTagsTest {

    public static final String TAG = "лес";
    public static final String TAG_VALUE = "1794329";
    public static final String TAG_CHECKED = "Tag_checked";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Искать в описании объявления»")
    public void shouldSeeIncludeTagInUrl() {
        user.scrollToElement(user.onOffersSearchPage().extendFilters().includeTags());
        user.onOffersSearchPage().extendFilters().includeTags().sendKeys(TAG);
        user.onOffersSearchPage().extendFilters().suggest().waitUntil(hasSize(greaterThan(0))).get(FIRST).click();
        user.onOffersSearchPage().extendFilters().button(TAG).waitUntil(isDisplayed())
                .should(hasClass(containsString(TAG_CHECKED)));
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("includeTag", TAG_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Исключить, если в описании»")
    public void shouldSeeExcludeTagInUrl() {
        user.scrollToElement(user.onOffersSearchPage().extendFilters().includeTags());
        user.onOffersSearchPage().extendFilters().excludeTags().sendKeys(TAG);
        user.onOffersSearchPage().extendFilters().suggest().waitUntil(hasSize(greaterThan(0))).get(FIRST).click();
        user.onOffersSearchPage().extendFilters().button(TAG).waitUntil(isDisplayed())
                .should(hasClass(containsString(TAG_CHECKED)));
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("excludeTag", TAG_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
