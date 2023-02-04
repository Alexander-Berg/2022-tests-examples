package ru.yandex.general.adult;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FOR_ADULTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.INTIM_TOVARI;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.ADULT_CONFIRMED;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FOR_ADULTS_FEATURE)
@Feature("Сниппет «Только для взрослых», листинг списком")
@DisplayName("Сниппет «Только для взрослых», листинг списком")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AdultSnippetListListingTest {

    private static final String TRUE = "true";
    private static final String IM_18 = "Мне уже есть 18";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setSearch(listingCategoryResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).getMockSnippet().setCategoryForAdults(true))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        urlSteps.testing().path(INTIM_TOVARI);
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается иконка «18+» на сниппете на листинге списком")
    public void shouldSeeAdultAgeIconListListing() {
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Мне уже есть 18» на сниппете на листинге списком")
    public void shouldSeeIm18ButtonOnHoverListListing() {
        urlSteps.open();
        basePageSteps.onListingPage().snippetFirst().hover();

        basePageSteps.onListingPage().snippetFirst().button(IM_18).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проставляется кука по тапу на «Мне уже есть 18», пропадает иконка «18+», на листинге списком")
    public void shouldSeeCookieTapIm18ButtonListListing() {
        urlSteps.open();
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().snippetFirst().button(IM_18).hover().click();

        basePageSteps.shouldSeeCookie(ADULT_CONFIRMED, TRUE);
        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет иконки «18+» с кукой, на листинге списком")
    public void shouldNotSeeAdultAgeIconWithCookieListListing() {
        basePageSteps.setCookie(ADULT_CONFIRMED, TRUE);
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(not(isDisplayed()));
    }

}
