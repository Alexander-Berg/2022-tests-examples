package ru.auto.tests.mobilereviews.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;

@DisplayName("Ссылка «Все отзывы»")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AllReviewsUrlTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String allReviewsUrl;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/cars/aro/", "/cars/all/"},
                {"/cars/aro/10/", "/cars/aro/"},
                {"/cars/aro/10/20344427/", "/cars/aro/10/"},

                {"/moto/atv/aie_motor/", "/moto/atv/"},
                {"/moto/atv/aie_motor/4cross/", "/moto/atv/aie_motor/"},

                {"/moto/scooters/cpi/", "/moto/scooters/"},
                {"/moto/scooters/cpi/aragon/", "/moto/scooters/cpi/"},

                {"/moto/motorcycle/cz/", "/moto/motorcycle/"},
                {"/moto/motorcycle/cz/350/", "/moto/motorcycle/cz/"},

                {"/moto/snowmobile/bars/", "/moto/snowmobile/"},
                {"/moto/snowmobile/bars/sledopit/", "/moto/snowmobile/bars/"},

                {"/trucks/truck/avia/", "/trucks/truck/"},
                {"/trucks/truck/avia/d___series/", "/trucks/truck/avia/"},

                {"/trucks/lcv/ifa/", "/trucks/lcv/"},
                {"/trucks/lcv/ifa/multicar_25/", "/trucks/lcv/ifa/"},

                {"/trucks/municipal/mtz/", "/trucks/municipal/"},
                {"/trucks/municipal/mtz/mtz_82_municipal/", "/trucks/municipal/mtz/"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все отзывы»")
    @Category({Regression.class})
    public void shouldClickAllReviewsUrl() {
        urlSteps.testing().path(REVIEWS).path(startUrl).open();

        basePageSteps.onReviewsListingPage().allReviwesUrl().hover().click();
        urlSteps.testing().path(REVIEWS).path(allReviewsUrl)
                .addParam("sort", "relevance-exp1-desc").shouldNotSeeDiff();

    }
}
