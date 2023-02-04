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
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.PAGE_PUBLIC_PROFILE;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_BASIC_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Отправка «CARD_OFFER_CLICK» с публичного профиля юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class CardOfferClickPublicProfileGoalTest {

    private static final String REGION_ID = "2";
    private static final String ID = "123456";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        urlSteps.testing().path(PROFILE).path("testprofile");

        mockRule.graphqlStub(mockResponse().setPublicProfile(profileResponse().snippets(asList(
                mockSnippet(PROFILE_BASIC_SNIPPET).setId(ID))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);

        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_CLICK)
    @DisplayName("Отправка «CARD_OFFER_CLICK» с публичного профиля юзера")
    public void shouldSeeCardOfferClickGoalFromPublicProfile() {
        basePageSteps.onListingPage().snippetFirst().waitUntil(isDisplayed()).hover().click();

        goalsSteps.withGoalType(CARD_OFFER_CLICK)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setOfferId(ID)
                        .setRegionId(REGION_ID)
                        .setBlock(BLOCK_LISTING)
                        .setPage(PAGE_PUBLIC_PROFILE))
                .withCount(1)
                .shouldExist();
    }

}
