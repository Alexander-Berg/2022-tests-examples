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
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.Events.PAGE_CARD;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CARD_OFFER_SIMILAR_OFFERS_SHOW;
import static ru.yandex.general.consts.Goals.PHONE_CALL;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.page.OfferCardPage.CALL;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockChat.chatTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class OfferCardMockedGoalTest {

    private static final String ID = "123456";
    private static final String CATEGORY_ID = "mobilnie-telefoni_OobNbL";
    private static final String REGION_ID = "2";
    private static final String FEED = "Feed";
    private static final String FORM = "Form";

    private MockResponse mockResponse = mockResponse().setChat(chatTemplate().setIsNew(true).build())
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();
    private MockCard card;

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
        basePageSteps.setCookie(ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE, ru.yandex.general.step.BasePageSteps.TRUE);
        card = mockCard(BASIC_CARD).setId(ID).addSimilarOffers(1);
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PHONE_CALL)
    @DisplayName("Цель «PHONE_CALL», по «Показать телефон» на карточке оффера, оффер фидовый")
    public void shouldSeeCardPhoneCallGoalOnOpenFeedCard() {
        mockRule.graphqlStub(mockResponse
                .setCard(card.setOfferOrigin(FEED).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().link(CALL).click();

        goalsSteps.withGoalType(PHONE_CALL)
                .withBody(goalRequestBody().setOfferId(ID)
                        .setEventPlace(PAGE_CARD)
                        .setCategoryId(CATEGORY_ID)
                        .setRegionId(REGION_ID)
                        .setOfferOrigin(FEED))
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PHONE_CALL)
    @DisplayName("Цель «PHONE_CALL», по «Показать телефон» на карточке оффера, оффер ручной")
    public void shouldSeeCardPhoneCallGoalOnOpenFormCard() {
        mockRule.graphqlStub(mockResponse
                .setCard(card.setOfferOrigin(FORM).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().link(CALL).click();

        goalsSteps.withGoalType(PHONE_CALL)
                .withBody(goalRequestBody().setOfferId(ID)
                        .setEventPlace(PAGE_CARD)
                        .setCategoryId(CATEGORY_ID)
                        .setRegionId(REGION_ID)
                        .setOfferOrigin(FORM))
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(PHONE_CALL)
    @DisplayName("Нет цели «PHONE_CALL» при открытии карточки")
    public void shouldNotSeePhoneCallGoalOnOpenCard() {
        mockRule.graphqlStub(mockResponse
                .setCard(card.build())
                .build()).withDefaults().create();
        urlSteps.open();

        goalsSteps.withGoalType(PHONE_CALL)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(CARD_OFFER_SIMILAR_OFFERS_SHOW)
    @DisplayName("Цель «CARD_OFFER_SIMILAR_OFFERS_SHOW», при отображении блока похожих")
    public void shouldSeeCardOfferSimilarOffersShowGoal() {
        mockRule.graphqlStub(mockResponse
                .setCard(card.build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover();

        goalsSteps.withGoalType(CARD_OFFER_SIMILAR_OFFERS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
