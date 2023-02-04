package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@Feature(VIN)
@Story("Блок контактов")
@DisplayName("Отправка метрики при добавлении/удалении избранного")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryReportFavoriteMetricsTest {

    private static final String FAVORITE = "{\"proauto-report\":{\"favorite\":{\"add\":{},\"notification\":{},\"show\":{}}}}";
    private static final String REMOVE = "{\"proauto-report\":{\"favorite\":{\"remove\":{}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private SeleniumMockSteps browserMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid")
        ).create();

        urlSteps.testing().path(HISTORY).path("/1076842087-f1e84/");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка метрики при добавлении в избранное")
    public void shouldSeeMetricRequestFromAddFavorite() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/UserFavoritesCarsPost")
        ).update();

        urlSteps.open();

        basePageSteps.onHistoryPage().contacts().favoriteButton().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(FAVORITE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка метрики при удалении из избранного")
    public void shouldSeeMetricRequestFromDeleteFavorite() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUserIsFavoriteTrue"),
                stub("desktop/UserFavoritesCarsDelete")
        ).update();

        urlSteps.open();

        basePageSteps.onHistoryPage().contacts().favoriteButton().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(REMOVE)));
    }

}
