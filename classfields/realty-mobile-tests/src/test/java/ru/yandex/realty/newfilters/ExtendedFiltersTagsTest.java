package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры. Рядом")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersTagsTest {

    private static final String TAG = "парк";
    private static final String TAG_VALUE = "1794326";
    private static final String TAG_CHECKED = "Tag_checked";
    private static final String NEARBY = "Рядом";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.scrollToElement(
                basePageSteps.onMobileMainPage().extendFilters().byName(NEARBY));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Искать в описании объявления»")
    public void shouldSeeIncludeTagInUrl() {
        basePageSteps.onMobileMainPage().extendFilters().includeTags().click();
        basePageSteps.onMobileMainPage().extendFilters().filterPopup().input().sendKeys(TAG);
        basePageSteps.onMobileMainPage().extendFilters().filterPopup().item(TAG).click();
        basePageSteps.onMobileMainPage().extendFilters().button(TAG).waitUntil(isDisplayed())
                .should(hasClass(containsString(TAG_CHECKED)));
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("includeTag", TAG_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Исключить, если в описании»")
    public void shouldSeeExcludeTagInUrl() {
        basePageSteps.onMobileMainPage().extendFilters().excludeTags().click();
        basePageSteps.onMobileMainPage().extendFilters().filterPopup().input().sendKeys(TAG);
        basePageSteps.onMobileMainPage().extendFilters().filterPopup().item(TAG).click();
        basePageSteps.onMobileMainPage().extendFilters().button(TAG).waitUntil(isDisplayed())
                .should(hasClass(containsString(TAG_CHECKED)));
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("excludeTag", TAG_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
