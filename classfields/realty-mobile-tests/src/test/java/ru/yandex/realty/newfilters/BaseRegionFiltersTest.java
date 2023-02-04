package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;

@DisplayName("Базовые фильтры. Выбираем регион")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseRegionFiltersTest {

    private static final String REGION_TO_CHOOSE = "Екатеринбург";

    private String oldRegion;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());

        oldRegion = user.onMobileMainPage().searchFilters().region().value().getText();
        user.onMobileMainPage().searchFilters().region().click();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Mobile.class})
    @DisplayName("Выбран дефолтный регион")
    public void shouldSeeDefaultRegion() {
        user.onMobileMainPage().searchFilters().filterPopup().selectedRegion().should(hasText(oldRegion));
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Mobile.class})
    @DisplayName("Ещё раз выбираем дефолтный регион")
    public void shouldChooseDefaultRegion() {
        user.onMobileMainPage().searchFilters().filterPopup().selectedRegion().click();
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Mobile.class})
    @DisplayName("Меняем регион на пердложенный")
    public void shouldClearFromPrice() {
        String newRegion = user.onMobileMainPage().searchFilters().filterPopup().selectFirstRegion();
        user.onMobileMainPage().searchFilters().region().value().should(hasText(newRegion));
        shouldSeeRegion(not(newRegion));
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Mobile.class})
    @DisplayName("Ищем регион в поиске")
    public void shouldSearchRegion() {
        user.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(REGION_TO_CHOOSE);
        user.onMobileMainPage().searchFilters().filterPopup().item(REGION_TO_CHOOSE).click();
        user.onMobileMainPage().searchFilters().region().value().should(hasText(findPattern(REGION_TO_CHOOSE)));
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Mobile.class})
    @DisplayName("Смотрим, что меняется регион в урле")
    public void shouldSeeRegionInUrl() {
        user.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(REGION_TO_CHOOSE);
        user.onMobileMainPage().searchFilters().filterPopup().item(REGION_TO_CHOOSE).click();
        user.onMobileMainPage().searchFilters().region().value().should(hasText(findPattern(REGION_TO_CHOOSE)));
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.testing().path("ekaterinburg").path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Step("Должен быть выбран регион «{region}»")
    public void shouldSeeRegion(Matcher region) {
        assertThat("Должен быть регион", oldRegion, region);
    }
}
