package ru.yandex.general.goals;

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
import ru.yandex.general.mock.MockListingSnippet;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FAVOURITES_OFFERS_ADD;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockHomepage.homepageResponse;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Отправка «FAVOURITES_OFFERS_ADD» с разных типов листингов")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingAddToFavouritesGoalTest {

    private static final String REGION_ID = "2";
    private static final String ID = "123456";
    private static final String CARD_ID = "1111111";
    private static final String CATEGORY_ID = "koshki_oyCgxy";

    private static MockListingSnippet snippet = mockSnippet(BASIC_SNIPPET).getMockSnippet()
            .setId(ID)
            .setCategoryId(CATEGORY_ID);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String eventPlace;

    @Parameterized.Parameter(3)
    public MockResponse mockResponse;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Отправка события с главной", SLASH, "PageMain",
                        mockResponse().setHomepage(homepageResponse().offers(asList(snippet)).build())
                },
                {"Отправка события с листинга категории", ELEKTRONIKA, "PageListing",
                        mockResponse().setSearch(listingCategoryResponse().offers(asList(snippet)).build()),
                },
                {"Отправка события с листинга похожих офферов", CARD + CARD_ID + SLASH, "PageCard",
                        mockResponse().setCard(mockCard(BASIC_CARD).setId(CARD_ID).similarOffers(asList(snippet)).build()),
                }
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(path);
        passportSteps.commonAccountLogin();

        mockRule.graphqlStub(mockResponse
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);

        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVOURITES_OFFERS_ADD)
    @DisplayName("Отправка «FAVOURITES_OFFERS_ADD» с разных типов листингов")
    public void shouldSeeAddOfferToFavouritesGoalFromListings() {
        basePageSteps.onListingPage().snippetFirst().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();

        goalsSteps.withGoalType(FAVOURITES_OFFERS_ADD)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setOfferId(ID)
                        .setCategoryId(CATEGORY_ID)
                        .setRegionId(REGION_ID)
                        .setEventPlace(eventPlace))
                .withCount(1)
                .shouldExist();
    }

}
