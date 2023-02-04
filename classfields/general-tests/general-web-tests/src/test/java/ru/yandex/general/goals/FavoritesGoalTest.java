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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_CLICK;
import static ru.yandex.general.consts.Goals.FAVOURITES_OFFERS_SHOW;
import static ru.yandex.general.consts.Goals.FAVOURITES_SEARCHES_CLICK;
import static ru.yandex.general.consts.Goals.FAVOURITES_SEARCHES_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на страничке избранного")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FavoritesGoalTest {

    private static final String REGION_ID = "2";

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
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        passportSteps.commonAccountLogin();

        mockRule.graphqlStub(mockResponse().setFavoritesExample()
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        urlSteps.testing().path(MY).path(FAVORITES);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVOURITES_OFFERS_SHOW)
    @DisplayName("Цель «FAVOURITES_OFFERS_SHOW» при открытии избранных офферов")
    public void shouldSeeFavouritesOffersShowGoal() {
        urlSteps.open();

        goalsSteps.withGoalType(FAVOURITES_OFFERS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVOURITES_SEARCHES_SHOW)
    @DisplayName("Цель «FAVOURITES_SEARCHES_SHOW» при открытии сохраненных поисков")
    public void shouldSeeFavouritesSearchesShowGoal() {
        urlSteps.queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();

        goalsSteps.withGoalType(FAVOURITES_SEARCHES_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_CLICK)
    @DisplayName("Цель «CARD_OFFER_CLICK» при переходе по офферу из избранного")
    public void shouldSeeCardOfferClickGoal() {
        urlSteps.open();
        basePageSteps.onFavoritesPage().firstFavCard().click();

        goalsSteps.withGoalType(CARD_OFFER_CLICK)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setOfferId("20471133654708224")
                        .setCategoryId("rezume-transport-logistika-ved-rabotnik-sklada_BLg2wi")
                        .setRegionId(REGION_ID)
                        .setBlock("BlockListing")
                        .setPage("PageFavorites"))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVOURITES_SEARCHES_CLICK)
    @DisplayName("Цель «FAVOURITES_SEARCHES_CLICK» при переходе по поиску из избранного")
    public void shouldSeeFavouritesSearchesClickGoal() {
        urlSteps.queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
        basePageSteps.onFavoritesPage().firstFavCard().click();

        goalsSteps.withGoalType(FAVOURITES_SEARCHES_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
