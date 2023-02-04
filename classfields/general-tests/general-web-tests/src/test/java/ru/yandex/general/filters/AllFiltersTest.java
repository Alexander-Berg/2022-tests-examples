package ru.yandex.general.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.element.FilterChip.RESET_ALL;
import static ru.yandex.general.page.ListingPage.APPLY;
import static ru.yandex.general.page.ListingPage.CONDITION;
import static ru.yandex.general.page.ListingPage.PRICE;
import static ru.yandex.general.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Базовые проверки")
@DisplayName("Фильтры. Попап все фильтры")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AllFiltersTest {

    private static final String PRODUCER = "Производитель";
    private static final String TEST_PRODUCER = "Apple";
    private static final String FIRST_CATEGORY = "8 ГБ";
    private static final String FIRST_URL_CATEGORY = "8-gb";
    private static final String SECOND_CATEGORY = "4 ГБ";
    private static final String SECOND_URL_CATEGORY = "4-gb";
    private static final String NOVIY = "Новый";
    private static final String OFFER_STATE = "offer.state";
    private static final String NEW = "new";
    private static final String MAX_PRICE = "300000";
    private static final String MIN_PRICE = "1";

    private static final String FILTER_CATEGORY = "offer.attributes.operativnaya-pamyat_15938685_serJvE";
    private static final String CATEGORY_FILTER = "Оперативная память";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI);
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Открывается попап «Все фильтры»")
    public void shouldSeeAllFiltersPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();

        basePageSteps.onListingPage().allFiltersPopup()
                .should(isDisplayed()).should(hasText(containsString("Все фильтры")));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Закрывается попап «Все фильтры»")
    public void shouldCloseAllFiltersPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().allFiltersPopup().closeAllFiltersPopupButton().click();

        basePageSteps.onListingPage().allFiltersPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Жмем «показать все» в фильтрах «Производитель»")
    public void shouldSeeMoreCategories() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        int defaultSize = basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER).inputList().size();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER).showAll().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER)
                .inputList().should(hasSize(greaterThan(defaultSize)));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Кнопка сбросить замьючена")
    public void shouldSeeMutedCancelButton() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();

        basePageSteps.onListingPage().allFiltersPopup().cancel()
                .should(hasAttribute("aria-disabled", "true")).should(hasText("Сбросить"));

    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Выбираем фильтр -> у кнопки сбросить появляется каунтер")
    public void shouldSeeActiveCancelButton() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock("Состояние").checkboxWithLabel("Б/У").click();

        basePageSteps.onListingPage().allFiltersPopup().cancel()
                .should(hasAttribute("aria-disabled", "false")).should(hasText("Сбросить 1 фильтр"));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Сбрасываем фильтр в попапе расширенных фильтров")
    public void shouldSeeCancelFilter() {
        urlSteps.path(STATE_NEW).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().cancel().click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр по двум производителям в расширенных фильтрах")
    public void shouldSeeTwoProducersFilterInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CATEGORY_FILTER).checkboxWithLabel(FIRST_CATEGORY)
                .click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CATEGORY_FILTER).checkboxWithLabel(SECOND_CATEGORY)
                .click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        basePageSteps.onListingPage().filters().chips("Оперативная память: 4 ГБ, 8 ГБ").should(isDisplayed());
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(FILTER_CATEGORY, SECOND_URL_CATEGORY).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр по производителю + фильтр по состоянию в расширенных фильтрах, смена каунтера")
    public void shouldSeeProducerAndConditionFilterInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CATEGORY_FILTER).checkboxWithLabel(FIRST_CATEGORY)
                .click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(hasText("3"));
        basePageSteps.onListingPage().filters().chips("Оперативная память: 8 ГБ").should(isDisplayed());
        basePageSteps.onListingPage().filters().chips("Состояние: Новый").should(isDisplayed());
        basePageSteps.onListingPage().filters().chips("Цена: до 300 000").should(isDisplayed());
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(STATE_NEW)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем 3 фильтра через чипсину «Сбросить все»")
    public void shouldResetThreeFiltersByResetAllChips() {
        urlSteps.path(STATE_NEW).queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).open();
        basePageSteps.onListingPage().filters().chips(RESET_ALL).click();


        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем один из 3 фильтров по крестику на чипсине")
    public void shouldResetOneOfThreeFilter() {
        urlSteps.path(STATE_NEW).queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).reset().click();


        basePageSteps.onListingPage().filters().counter().should(hasText("2"));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(2));
        basePageSteps.onListingPage().filters().chips(PRICE).should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(STATE_NEW)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем один из 3 фильтров в попапе на чипсине")
    public void shouldChangeOneOfThreeFilter() {
        urlSteps.path(STATE_NEW).queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).click();
        basePageSteps.onListingPage().popup().input("до").clearInput().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().input("от").sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().counter().should(hasText("3"));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(3));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(STATE_NEW)
                .queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открываем страницу с фильтрами -> спустя 5 секунд они остаются примененными")
    public void shouldSeeFiltersNotResetting() {
        urlSteps.path(STATE_NEW).queryParam(FILTER_CATEGORY, FIRST_URL_CATEGORY)
                .queryParam(PRICE_MAX_URL_PARAM, "300000").open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.waitSomething(5, TimeUnit.SECONDS);

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).should(
                hasValue("300 000"));
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CATEGORY_FILTER).checkboxWithLabel(FIRST_CATEGORY)
                .input().should(hasAttribute("checked", "true"));
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel("Новый").active()
                .should(isDisplayed());
    }

}
