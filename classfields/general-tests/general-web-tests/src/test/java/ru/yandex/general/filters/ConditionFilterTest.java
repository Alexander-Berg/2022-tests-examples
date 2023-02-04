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

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.Pages.STATE_USED;
import static ru.yandex.general.element.FilterChip.RESET_ALL;
import static ru.yandex.general.page.ListingPage.APPLY;
import static ru.yandex.general.page.ListingPage.CONDITION;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры состояния")
@DisplayName("Фильтры. Состояние")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ConditionFilterTest {

    private static final String BU = "Б/У";
    private static final String NOVIY = "Новый";
    private static final String OFFER_STATE = "offer.state";
    private static final String CATEGORY_PATH = "/noutbuki/";
    private static final String NEW = "new";
    private static final String USED = "used";
    private static final String CONDITION_NEW = "Состояние: Новый";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH);
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Б/У» видим в урле")
    public void shouldSeeUsedFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        basePageSteps.onListingPage().filters().chips("Состояние: Б/У").should(isDisplayed());
        urlSteps.path(STATE_USED).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Новый» видим в урле")
    public void shouldSeeNewFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        basePageSteps.onListingPage().filters().chips(CONDITION_NEW).should(isDisplayed());
        urlSteps.path(STATE_NEW).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтр «Новый + Б/У» видим в урле")
    public void shouldSeeNewUsedFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        basePageSteps.onListingPage().filters().chips("Состояние: Новый, Б/У").should(isDisplayed());
        urlSteps.queryParam(OFFER_STATE, NEW).queryParam(OFFER_STATE, USED).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим активированный фильтр «Б/У» в попапе фильтров")
    public void shouldSeeCheckedUsedFilter() {
        urlSteps.path(STATE_USED).open();
        basePageSteps.onListingPage().openExtFilter();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(BU).active()
                .should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим активированный фильтр «Новый» в попапе фильтров")
    public void shouldSeeCheckedNewFilter() {
        urlSteps.path(STATE_NEW).open();
        basePageSteps.onListingPage().openExtFilter();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).active()
                .should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим активированные фильтры «Новый» и «Б/У» в попапе фильтров")
    public void shouldSeeCheckedNewUsedFilter() {
        urlSteps.queryParam(OFFER_STATE, NEW).queryParam(OFFER_STATE, USED).open();
        basePageSteps.onListingPage().openExtFilter();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(BU).active()
                .should(isDisplayed());
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).active()
                .should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Скидываем фильтр «Новый» в попапе фильтров")
    public void shouldSeeCloseConditionFilter() {
        urlSteps.queryParam(OFFER_STATE, NEW).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Жмем закрытие попапа фильтров -> фильтр не применяется")
    public void shouldSeeCancelFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().closeAllFiltersPopupButton().click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скидываем фильтр «Новый» по крестику в чипсине")
    public void shouldSeeCancelNewFilter() {
        urlSteps.queryParam(OFFER_STATE, NEW).open();
        basePageSteps.onListingPage().filters().chips(CONDITION_NEW).reset().click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скидываем фильтр «Новый» по чипсине «Сбросить всё»")
    public void shouldSeeCancelNewFilterByResetAllChip() {
        urlSteps.queryParam(OFFER_STATE, NEW).open();
        basePageSteps.onListingPage().filters().chips(RESET_ALL).click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр «Б/у» из выпадающего списка по чипсине фильтра «Новый»")
    public void shouldAddUsedFilterInDropdownValueList() {
        urlSteps.queryParam(OFFER_STATE, NEW).open();
        basePageSteps.onListingPage().filters().chips(CONDITION_NEW).click();
        basePageSteps.onListingPage().popup().checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).queryParam(OFFER_STATE, NEW).queryParam(OFFER_STATE, USED)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем фильтр «Новый» на «Б/у» из выпадающего списка по чипсине фильтра «Новый»")
    public void shouldChangeNewToUsedFilterInDropdownValueList() {
        urlSteps.queryParam(OFFER_STATE, NEW).open();
        basePageSteps.onListingPage().filters().chips(CONDITION_NEW).click();
        basePageSteps.onListingPage().popup().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().popup().checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).path(STATE_USED)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтр «Новый» в выпадающем списке по чипсине фильтра «Новый»")
    public void shouldResetNewFilterInDropdownValueList() {
        urlSteps.queryParam(OFFER_STATE, NEW).open();
        basePageSteps.onListingPage().filters().chips(CONDITION_NEW).click();
        basePageSteps.onListingPage().popup().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(CATEGORY_PATH).shouldNotDiffWithWebDriverUrl();
    }

}
