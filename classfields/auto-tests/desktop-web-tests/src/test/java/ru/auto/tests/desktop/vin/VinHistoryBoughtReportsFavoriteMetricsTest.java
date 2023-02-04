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
import ru.auto.tests.desktop.mock.MockStub;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static io.restassured.http.Method.DELETE;
import static io.restassured.http.Method.POST;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.element.history.BoughtReport.IN_FAVORITE;
import static ru.auto.tests.desktop.element.history.BoughtReport.TO_FAVORITE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.boughtReportRawExample;
import static ru.auto.tests.desktop.mock.MockCarfaxReportsList.carfaxReportsResponse;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CARFAX_BOUGHT_REPORTS_RAW;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;

@Feature(VIN)
@Story("Купленные отчёты")
@DisplayName("Отправка метрики при добавлении/удалении избранного")
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryBoughtReportsFavoriteMetricsTest {

    private static final String FAVORITE = "{\"proauto-landing\":{\"my_reports\":{\"favorite\":{\"add\":{}}}}}";
    private static final String REMOVE = "{\"proauto-landing\":{\"my_reports\":{\"favorite\":{\"remove\":{}}}}}";
    private static final String FAVORITES_CARS_PATH = "/1.0/user/favorites/cars/1076842087-f1e84";

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
        urlSteps.testing().path(HISTORY);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка метрики при добавлении в избранное")
    public void shouldSeeMetricRequestFromAddFavorite() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                getCarfaxBoughtReportWithIsFavorite(false),
                stub().withPredicateType(DEEP_EQUALS)
                        .withPath(FAVORITES_CARS_PATH)
                        .withMethod(POST)
                        .withStatusSuccessResponse()).create();
        urlSteps.open();

        basePageSteps.onHistoryPage().getBoughtReport(0).button(TO_FAVORITE).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(FAVORITE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка метрики при удалении из избранного")
    public void shouldSeeMetricRequestFromDeleteFavorite() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                getCarfaxBoughtReportWithIsFavorite(true),
                stub().withPredicateType(DEEP_EQUALS)
                        .withPath(FAVORITES_CARS_PATH)
                        .withMethod(DELETE)
                        .withStatusSuccessResponse()).create();
        urlSteps.open();

        basePageSteps.onHistoryPage().getBoughtReport(0).button(IN_FAVORITE).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(REMOVE)));
    }

    private static MockStub getCarfaxBoughtReportWithIsFavorite(boolean isFavorite) {
        return stub().withGetDeepEquals(CARFAX_BOUGHT_REPORTS_RAW)
                .withRequestQuery(query().setPageSize("10"))
                .withResponseBody(carfaxReportsResponse().setReports(
                        boughtReportRawExample().setIsFavorite(isFavorite)).build());
    }

}
