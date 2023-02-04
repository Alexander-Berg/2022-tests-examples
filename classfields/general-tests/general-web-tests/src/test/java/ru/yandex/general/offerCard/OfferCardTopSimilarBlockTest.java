package ru.yandex.general.offerCard;

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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Блок похожих сверху")
@DisplayName("Блок похожих сверху")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardTopSimilarBlockTest {

    private static final String ID = "12345";

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
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается блок похожих сверху с 5 айтемами")
    public void shouldSeeNoSimilarCarouseCardItemsWith5Similar() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).addSimilarOffers(5).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();

        basePageSteps.onOfferCardPage().similarCarouseItems().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается 6 айтема в блоке похожих сверху")
    public void shouldSee6SimilarCarouseCardItems() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).addSimilarOffers(6).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();

        basePageSteps.onOfferCardPage().similarCarouseItems().should(hasSize(6));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается 30 айтемов в блоке похожих сверху, при 40 пришедших")
    public void shouldSee30SimilarCarouseCardItemsWith40Similar() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).addSimilarOffers(40).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();

        basePageSteps.onOfferCardPage().similarCarouseItems().should(hasSize(30));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается блок похожих сверху без скрола")
    public void shouldNotSeeSimilarCarouseCardItemsWithoutScroll() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).addSimilarOffers(8).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().similarCarouseItems().should(not(isDisplayed()));
    }

}
