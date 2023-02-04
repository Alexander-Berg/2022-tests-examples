package ru.auto.tests.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.GL;
import static ru.auto.tests.desktop.consts.QueryParams.OPENED_FEATURES_MODAL;

@DisplayName("AMP - листинг")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ReviewsListingTest {

    private static final String MARK = "land_rover";
    private static final String MODEL = "discovery";
    private static final String GEN = "2307388";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(AMP).path(CARS).path(MARK).path(MODEL).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviews().button("Все отзывы"));
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(SLASH)
                .addParam(FROM, "listing").ignoreParam(GL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class})
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviews().getReview(0));
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/7156746515365802633/")
                .ignoreParam(GL).addParam(FROM, "card").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по ссылке «Все плюсы и минусы»")
    public void shouldClickAllPlusUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().button("Все плюсы и минусы"));
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(ALL)
                .ignoreParam(GL).addParam(OPENED_FEATURES_MODAL, "true").shouldNotSeeDiff();
    }
}
