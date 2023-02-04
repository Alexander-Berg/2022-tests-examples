package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.MockStub;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.articleExample;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.reviewExample;
import static ru.auto.tests.desktop.mock.MockLentaFeed.feedTemplate;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.LENTA_GET_FEED;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;

@DisplayName("Лента в гараже")
@Epic(AutoruFeatures.GARAGE)
@Feature("Лента")
@Story("Статьи и отзывы")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageArticleAndReviewClickTest {

    private static final String GARAGE_CARD_ID = getRandomId();

    private static MockStub feedStub;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        feedStub = stub().withGetDeepEquals(LENTA_GET_FEED)
                .withRequestQuery(
                        query().setUserId("user:11604617")
                                .setContentAmount("3")
                                .setSource("ALL"));

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCardOffer().setId(GARAGE_CARD_ID).getBody())
        );

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переходим на статью")
    public void shouldGoToArticle() {
        String url = "audia4b8restyledurability123";

        mockRule.setStubs(
                feedStub.withResponseBody(
                        feedTemplate().setPayloads(
                                articleExample()
                                        .setUrl(url)
                        ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onGarageCardPage().articlesAndReviews().get(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(url).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переходим на отзыв")
    public void shouldGoToReview() {
        String url = "/review/cars/audi/a4/20637504/5706867550556877623";

        mockRule.setStubs(
                feedStub.withResponseBody(
                        feedTemplate().setPayloads(
                                reviewExample()
                                        .setUrl(url)
                        ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onGarageCardPage().articlesAndReviews().get(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(url).path(SLASH).shouldNotSeeDiff();
    }

}
