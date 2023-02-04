package ru.auto.tests.mobile.vin;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;

@Feature(VIN)
@Story("Купленные отчёты")
@DisplayName("Отправка метрики при добавлении/удалении избранного")
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryBoughtReportsFavoriteMetricsTest {

    private static final String FAVORITE = "{\"proauto-landing\":{\"my_reports\":{\"favorite\":{\"add\":{}}}}}";
    private static final String REMOVE = "{\"proauto-landing\":{\"my_reports\":{\"favorite\":{\"remove\":{}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        urlSteps.testing().path(HISTORY);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка метрики при добавлении в избранное")
    public void shouldSeeMetricRequestFromAddFavorite() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxBoughtReportsRawIsFavoriteFalse",
                "desktop/UserFavoritesCarsPost").post();
        urlSteps.open();

        basePageSteps.onHistoryPage().getBoughtReport(0).favoriteButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(FAVORITE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка метрики при удалении из избранного")
    public void shouldSeeMetricRequestFromDeleteFavorite() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxBoughtReportsRawIsFavoriteTrue",
                "desktop/UserFavoritesCarsDelete").post();
        urlSteps.open();

        basePageSteps.onHistoryPage().getBoughtReport(0).favoriteButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(REMOVE)));
    }

}
