package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffers.offersExample;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.PERSONALIZATION_GET_OFFERS_FEED;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок «Рекомендации»")
@Feature(AutoruFeatures.MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class RecomendationsTest {

    private static final String SALE_ID = "1114782787-3301e085";
    private static final String PAGE_SIZE = "20";

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
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub().withGetDeepEquals(PERSONALIZATION_GET_OFFERS_FEED)
                        .withRequestQuery(query().setPage("1").setPageSize(PAGE_SIZE))
                        .withResponseBody(offersExample().build())
        ).create();

        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по сниппету из блока «Рекомендации»")
    public void shouldGoToRecomendationSnippet() {
        mockRule.setStubs(stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                .withResponseBody(mockOffer(CAR_EXAMPLE).getResponse())
        ).update();

        basePageSteps.scrollAndClick(basePageSteps.onMainPage().snippets().get(0).waitUntil(isDisplayed()));
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("honda").path("shuttle").path(SALE_ID).path(SLASH)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("В блоке «Рекомендации» отображается 20 офферов")
    public void shouldSeeFirstPageSnippets() {
        basePageSteps.onMainPage().snippets().should(hasSize(20));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("При скролле блока «Рекомендации» подгружается вторая страница")
    public void shouldSeeSecondPageSnippets() {
        mockRule.overwriteStub(1, stub().withGetDeepEquals(PERSONALIZATION_GET_OFFERS_FEED)
                .withRequestQuery(query().setPage("2").setPageSize(PAGE_SIZE))
                .withResponseBody(offersExample().build()));

        basePageSteps.onMainPage().snippets().get(16).hover();

        basePageSteps.onMainPage().snippets().should(hasSize(40));
    }

}
