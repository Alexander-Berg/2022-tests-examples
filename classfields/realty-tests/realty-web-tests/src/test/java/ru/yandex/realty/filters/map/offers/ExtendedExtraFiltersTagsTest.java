package ru.yandex.realty.filters.map.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Карта. Расширенные фильтры поиска по объявлениям.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedExtraFiltersTagsTest {

    private static final String TAG = "лес";
    private static final String TAG_VALUE = "1794329";
    private static final String TAG_CHECKED = "Tag_checked";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Искать в описании объявления»")
    public void shouldSeeIncludeTagInUrl() {
        basePageSteps.onMapPage().extendFilters().includeTags().sendKeys(TAG);
        basePageSteps.onMapPage().extendFilters().suggest().waitUntil(hasSize(greaterThan(FIRST))).get(FIRST).click();
        basePageSteps.onMapPage().extendFilters().button(TAG).waitUntil(isDisplayed())
                .should(hasClass(containsString(TAG_CHECKED)));
        basePageSteps.loaderWait();
        urlSteps.queryParam("includeTag", TAG_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Исключить, если в описании»")
    public void shouldSeeExcludeTagInUrl() {
        basePageSteps.onMapPage().extendFilters().excludeTags().sendKeys(TAG);
        basePageSteps.onMapPage().extendFilters().suggest().waitUntil(hasSize(greaterThan(FIRST))).get(FIRST).click();
        basePageSteps.onMapPage().extendFilters().button(TAG).waitUntil(isDisplayed())
                .should(hasClass(containsString(TAG_CHECKED)));
        basePageSteps.loaderWait();
        urlSteps.queryParam("excludeTag", TAG_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
