package ru.yandex.general.adult;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockListingSnippet;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FOR_ADULTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.INTIM_TOVARI;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mobile.step.BasePageSteps.ADULT_CONFIRMED;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockHomepage.homepageResponse;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FOR_ADULTS_FEATURE)
@Feature("Сниппет «Только для взрослых», листинг плиткой")
@DisplayName("Сниппет «Только для взрослых»")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdultSnippetGridListingTest {

    private static final String CARD_ID = "12345";
    private static final String TRUE = "true";
    private static final String IM_18 = "Мне уже есть 18";

    private static MockListingSnippet snippet = mockSnippet(BASIC_SNIPPET).getMockSnippet().setCategoryForAdults(true);

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

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public MockResponse mockResponse;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Сниппет для взрослых на главной", SLASH,
                        mockResponse().setHomepage(homepageResponse().offers(asList(snippet)).build()),
                },
                {"Сниппет для взрослых на листинге", INTIM_TOVARI,
                        mockResponse().setSearch(listingCategoryResponse().offers(asList(snippet)).build())
                },
                {"Сниппет для взрослых на похожих карточки оффера", CARD + CARD_ID + SLASH,
                        mockResponse().setCard(mockCard(BASIC_CARD).setId(CARD_ID).similarOffers(asList(snippet)).build())
                }
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(path);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются иконка «18+» и кнопка «Мне уже есть 18» на сниппете на главной/листинге/похожих")
    public void shouldSeeAdultAgeIconAndButton() {
        urlSteps.open();
        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(isDisplayed());
        basePageSteps.onListingPage().snippetFirst().button(IM_18).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проставляется кука по тапу на «Мне уже есть 18», пропадает иконка «18+», на главной/листинге/похожих")
    public void shouldSeeCookieTapIm18Button() {
        urlSteps.open();
        basePageSteps.scrollingToElement(basePageSteps.onListingPage().snippetFirst());
        basePageSteps.onListingPage().snippetFirst().button(IM_18).click();

        basePageSteps.shouldSeeCookie(ADULT_CONFIRMED, TRUE);
        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет иконки «18+» с кукой, на главной/листинге/похожих")
    public void shouldNotSeeAdultAgeIconWithCookie() {
        basePageSteps.setCookie(ADULT_CONFIRMED, TRUE);
        urlSteps.open();

        basePageSteps.onListingPage().snippetFirst().adultAgeIcon().should(not(isDisplayed()));
    }

}
