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
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FAVOURITES_OFFERS_ADD;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class OfferCardAddToFavouritesGoalTest {

    private static final String REGION_ID = "2";
    private static final String ID = "123456";
    private static final String CATEGORY_ID = "koshki_oyCgxy";

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
        urlSteps.testing().path(CARD).path(ID);
        passportSteps.commonAccountLogin();

        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setId(ID).setCategoryId(CATEGORY_ID).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);

        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVOURITES_OFFERS_ADD)
    @DisplayName("Отправка «FAVOURITES_OFFERS_ADD» с карточки оффера")
    public void shouldSeeAddOfferToFavouritesGoalFromOfferCard() {
        basePageSteps.onOfferCardPage().addToFavorite().click();

        goalsSteps.withGoalType(FAVOURITES_OFFERS_ADD)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setOfferId(ID)
                        .setCategoryId(CATEGORY_ID)
                        .setRegionId(REGION_ID)
                        .setEventPlace("PageCard"))
                .withCount(1)
                .shouldExist();
    }

}
