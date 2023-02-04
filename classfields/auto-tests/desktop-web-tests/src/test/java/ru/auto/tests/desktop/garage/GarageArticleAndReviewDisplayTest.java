package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.mock.MockStub;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.ARTICLE_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.REVIEW_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockLentaFeedPayload.feedPayload;
import static ru.auto.tests.desktop.mock.MockLentaFeed.feedTemplate;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.LENTA_GET_FEED;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.auto.tests.desktop.utils.Utils.getRuDateByPattern;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Лента в гараже")
@Epic(AutoruFeatures.GARAGE)
@Feature("Лента")
@Story("Статьи и отзывы")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GarageArticleAndReviewDisplayTest {

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

    @Parameterized.Parameter
    public String type;

    @Parameterized.Parameter(1)
    public String mock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Новости", ARTICLE_EXAMPLE},
                {"Отзывы", REVIEW_EXAMPLE}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCardOffer().setId(GARAGE_CARD_ID).getBody())
        );

        feedStub = stub()
                .withGetDeepEquals(LENTA_GET_FEED)
                .withRequestQuery(
                        query().setUserId("user:11604617")
                                .setContentAmount("3")
                                .setSource("ALL"));

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается заголовок новости/отзыва")
    public void shouldSeeTitle() {
        String title = getRandomString();

        mockRule.setStubs(
                feedStub.withResponseBody(
                        feedTemplate().setPayloads(
                                feedPayload(mock)
                                        .setTitle(title)
                        ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onGarageCardPage().articlesAndReviews().get(0).title().should(hasText(title));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются тип и дата новости/отзыва")
    public void shouldSeeStats() {
        int daysSinceToday = getRandomBetween(20, 60);

        mockRule.setStubs(
                feedStub.withResponseBody(
                        feedTemplate().setPayloads(
                                feedPayload(mock)
                                        .setCreatedDate(daysSinceToday)
                        ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onGarageCardPage().articlesAndReviews().get(0).stats().should(hasText(
                format("%s • %s", type, getRuDateByPattern(daysSinceToday, "dd MMMM y"))));
    }

}
