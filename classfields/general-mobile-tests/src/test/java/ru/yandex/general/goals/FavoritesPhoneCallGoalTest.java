package ru.yandex.general.goals;

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
import ru.yandex.general.mock.MockFavoritesSnippet;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.Events.PAGE_FAVORITES;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.PHONE_CALL;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@Feature(PHONE_CALL)
@DisplayName("Цель «PHONE_CALL»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FavoritesPhoneCallGoalTest {

    private static final String PHONE = "+79118887766";
    private static final String ID = "123456";
    private static final String CATEGORY_ID = "mobilnie-telefoni_OobNbL";
    private static final String REGION_ID = "2";
    private static final String FEED = "Feed";
    private static final String FORM = "Form";

    private MockResponse mockResponse = mockResponse().setOfferPhone(PHONE)
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();
    private MockFavoritesSnippet favoritesSnippet;

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

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        favoritesSnippet = mockSnippet(BASIC_SNIPPET).getMockSnippet().setCategoryId(CATEGORY_ID).setId(ID);
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        urlSteps.testing().path(MY).path(FAVORITES);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «PHONE_CALL», по «Показать телефон» на сниппете фидового оффера в избранном")
    public void shouldSeeFavoritesPhoneCallGoalFeedOffer() {
        mockRule.graphqlStub(mockResponse.setFavorites(
                        favoritesResponse().offers(asList(favoritesSnippet.setOfferOrigin(FEED))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFavoritesPage().firstFavCard().phoneShow().click();

        goalsSteps.withGoalType(PHONE_CALL)
                .withBody(goalRequestBody().setOfferId(ID)
                        .setEventPlace(PAGE_FAVORITES)
                        .setCategoryId(CATEGORY_ID)
                        .setRegionId(REGION_ID)
                        .setOfferOrigin(FEED))
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «PHONE_CALL», по «Показать телефон» на сниппете ручного оффера в избранном")
    public void shouldSeeFavoritesPhoneCallGoalFormOffer() {
        mockRule.graphqlStub(mockResponse.setFavorites(
                        favoritesResponse().offers(asList(favoritesSnippet.setOfferOrigin(FORM))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFavoritesPage().firstFavCard().phoneShow().click();

        goalsSteps.withGoalType(PHONE_CALL)
                .withBody(goalRequestBody().setOfferId(ID)
                        .setEventPlace(PAGE_FAVORITES)
                        .setCategoryId(CATEGORY_ID)
                        .setRegionId(REGION_ID)
                        .setOfferOrigin(FORM))
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «PHONE_CALL» при открытии избранных офферов")
    public void shouldNotSeeFavoritesPhoneCallGoalOnOpenPage() {
        mockRule.graphqlStub(mockResponse.setFavorites(
                        favoritesResponse().offers(asList(favoritesSnippet)).build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(PHONE_CALL)
                .withCount(0)
                .shouldExist();
    }

}
