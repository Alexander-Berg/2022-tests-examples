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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FOR_ADULTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.INTIM_TOVARI;
import static ru.yandex.general.mobile.step.BasePageSteps.ADULT_CONFIRMED;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.LIST;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FOR_ADULTS_FEATURE)
@Feature("Сниппет «Только для взрослых», листинг списком")
@DisplayName("Сниппет «Только для взрослых» в листинге списком")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class AdultSnippetListListingTest {

    private static final String TRUE = "true";

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

        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(INTIM_TOVARI);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются иконка «18+» на сниппете в листинге списком")
    public void shouldSeeAdultAgeIconAndButton() {
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет иконки «18+» с кукой, на сниппете в листинге списком")
    public void shouldNotSeeAdultAgeIconWithCookie() {
        basePageSteps.setCookie(ADULT_CONFIRMED, TRUE);
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(not(isDisplayed()));
    }

}
