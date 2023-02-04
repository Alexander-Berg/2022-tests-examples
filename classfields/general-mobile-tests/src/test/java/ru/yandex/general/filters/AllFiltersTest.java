package ru.yandex.general.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.mobile.page.ListingPage.PARAMETERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Базовые проверки")
@DisplayName("Фильтры. Попап все фильтры")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class AllFiltersTest {

    private static final String PRODUCER = "Производитель";
    private static final String TEST_PRODUCER = "Apple";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.resize(375, 1500);
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI);
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Открывается попап «Все фильтры»")
    public void shouldSeeAllFiltersPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters()
                .should(isDisplayed()).should(hasText(containsString(PARAMETERS)));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Закрывается попап «Все фильтры»")
    public void shouldCloseAllFiltersPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().waitUntil(isDisplayed());
        basePageSteps.onListingPage().filters().closePopup().click();
        basePageSteps.onListingPage().filters().should(not(isDisplayed()));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Ищем фильтрах «Производитель»")
    public void shouldFindCategory() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().inputWithFloatedPlaceholder(PRODUCER).click();
        basePageSteps.onListingPage().wrapper(PRODUCER).input("Найти").sendKeys(TEST_PRODUCER);
        basePageSteps.onListingPage().wrapper(PRODUCER).inputList().should(hasSize(2));
        basePageSteps.onListingPage().wrapper(PRODUCER).item(TEST_PRODUCER).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Кнопка сбросить замьючена")
    public void shouldSeeMutedCancelButton() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().cancel()
                .should(not(hasClass(containsString("FilterHeaderButton__active")))).should(hasText("Сбросить"));

    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Выбираем фильтр -> у кнопки сбросить появляется каунтер")
    public void shouldSeeActiveCancelButton() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel("Б/У").click();
        basePageSteps.onListingPage().filters().cancel()
                .should(hasClass(containsString("FilterHeaderButton__active"))).should(hasText("Сбросить 1"));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Сбрасываем фильтр в попапе расширенных фильтров")
    public void shouldSeeCancelFilter() {
        urlSteps.path(STATE_NEW).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().cancel().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

}
