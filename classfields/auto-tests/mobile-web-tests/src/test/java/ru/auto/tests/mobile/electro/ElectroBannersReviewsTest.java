package ru.auto.tests.mobile.electro;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.GASOLINE;
import static ru.auto.tests.desktop.consts.QueryParams.HYBRID;
import static ru.auto.tests.desktop.mock.MockReview.reviewExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.REVIEWS_AUTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.ELECTRO)
@Feature("Баннеры в отзывах")
@DisplayName("Баннеры в отзывах")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ElectroBannersReviewsTest {

    private static final String ID = "5413414149528081161";

    private static final String CATALOG_ELECTRO_BANNER = "Электромобили\nРассказываем — как выбрать, где заряжать " +
            "и о чём важно знать\nДавайте посмотрим";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в отзыве на авто с электро двигателем")
    public void shouldSeeElectroBannerInReviewWithElectroEngine() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", REVIEWS_AUTO, ID))
                        .withResponseBody(
                                reviewExample().setId(ID).setEngineType(QueryParams.ELECTRO).getBody()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(REVIEW).path(CARS).path(ID).path(SLASH).open();
        basePageSteps.onReviewPage().electroBanner().waitUntil(hasText(CATALOG_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в отзыве на авто с гибридным двигателем")
    public void shouldSeeElectroBannerInReviewithHybridEngine() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", REVIEWS_AUTO, ID))
                        .withResponseBody(
                                reviewExample().setId(ID).setEngineType(HYBRID).getBody()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(REVIEW).path(CARS).path(ID).path(SLASH).open();
        basePageSteps.onReviewPage().electroBanner().waitUntil(hasText(CATALOG_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет баннера «Электромобили» в отзыве на авто с бензиновым двигателем")
    public void shouldSeeNoElectroBannerInReviewithBenzinEngine() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", REVIEWS_AUTO, ID))
                        .withResponseBody(
                                reviewExample().setId(ID).setEngineType(GASOLINE).getBody()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(REVIEW).path(CARS).path(ID).path(SLASH).open();

        basePageSteps.onReviewPage().electroBanner().should(not(isDisplayed()));
    }

}
