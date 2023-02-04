package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockLentaFeed.feedExample;
import static ru.auto.tests.desktop.mock.MockLentaFeed.feedTemplate;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.REVIEW_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.articleExample;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.feedPayload;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.LENTA_GET_FEED;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.page.GarageCardPage.REVIEWS;
import static ru.auto.tests.desktop.page.GarageCardPage.WATCH_MORE;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Лента в гараже")
@Epic(AutoruFeatures.GARAGE)
@Feature("Лента")
@Story("Контролы ленты")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GarageFeedWatchMoreOtherTabsTest {

    private static final String GARAGE_CARD_ID = getRandomId();
    private static final String CONTENT_ID = format("magazine_%d", getRandomBetween(1000, 10000));
    private static final Query query = query();

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

    @Parameterized.Parameter
    public String tab;

    @Parameterized.Parameter(1)
    public String source;

    @Parameterized.Parameter(2)
    public String mock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
//                {ARTICLES, "MAGAZINE", ARTICLE_EXAMPLE},
                {REVIEWS, "REVIEWS", REVIEW_EXAMPLE}
        });
    }

    @Before
    public void before() {
        query.setUserId("user:11604617")
                .setContentAmount("3");

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCardOffer().setId(GARAGE_CARD_ID).getBody()),

                stub().withGetDeepEquals(LENTA_GET_FEED)
                        .withRequestQuery(
                                query.setSource("ALL"))
                        .withResponseBody(
                                feedTemplate().setPayloads(
                                        articleExample()
                                ).build())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).open();
        basePageSteps.onGarageCardPage().articlesAndReviews().waitUntil(hasSize(1));

        mockRule.overwriteStub(2,
                stub().withGetDeepEquals(LENTA_GET_FEED)
                        .withRequestQuery(
                                query.setSource(source))
                        .withResponseBody(
                                feedExample()
                                        .setId(2, CONTENT_ID)
                                        .build())
        );

        basePageSteps.onGarageCardPage().radioButton(tab).click();
        basePageSteps.onGarageCardPage().articlesAndReviews().waitUntil(hasSize(3));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Смотреть еще» с табов «Статьи/Отзывы»")
    public void shouldClickMore() {
        String title = getRandomString();

        mockRule.overwriteStub(2,
                stub().withGetDeepEquals(LENTA_GET_FEED)
                        .withRequestQuery(
                                query.setSource(source)
                                        .setContentAmount("5")
                                        .setContentId(CONTENT_ID))
                        .withResponseBody(
                                feedTemplate().setPayloads(
                                        feedPayload(mock)
                                                .setTitle(title)
                                ).build())
        );

        basePageSteps.onGarageCardPage().button(WATCH_MORE).click();

        basePageSteps.onGarageCardPage().articlesAndReviews().should(hasSize(4));
        basePageSteps.onGarageCardPage().articlesAndReviews().get(3).title().should(hasText(title));
    }

}
