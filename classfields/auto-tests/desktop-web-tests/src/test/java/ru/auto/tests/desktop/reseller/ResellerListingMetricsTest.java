package ru.auto.tests.desktop.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.noRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockSearchCars.SEARCH_PROFESSIONAL_SELLER_OFFER;
import static ru.auto.tests.desktop.mock.MockSearchCars.getSearchOffersUsedQuery;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.SEARCH_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;

@DisplayName("Переход на профиль перекупа с листинга")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Провязки с листинга")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class ResellerListingMetricsTest {

    private static final String LISTING_LINK_SHOW = "{\"cars\":{\"listing\":{\"reseller_public\":{\"link-show\":{}}}}}";
    private static final String LISTING_LINK_CLICK =
            "{\"cars\":{\"listing\":{\"reseller_public\":{\"link-click\":{}}}}}";

    private static final int PXLS_TO_OFFER = 400;
    private static final int PXLS_TO_CAROUSEL_OFFER = 650;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                getSearchOffersUsedQuery())
                        .withResponseBody(
                                searchOffers(SEARCH_PROFESSIONAL_SELLER_OFFER)
                                        .setEncryptedUserId(USER_ID).getBody()),
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase()))
                        .withResponseBody(
                                resellerOffersExample().build()),
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody())
        ).create();

        basePageSteps.setWindowHeight(800);

        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрика «link-show», при отображении ссылки на публичный профиль перекупа на снипете листинга")
    public void shouldSeeLinkShowMetricFromListing() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).open();
        basePageSteps.scrollDown(PXLS_TO_OFFER);

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(LISTING_LINK_SHOW)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет метрики «link-show», без отображения ссылки на публичный профиль перекупа на снипете листинга")
    public void shouldSeeNoLinkClickMetricFromListingWithoutShowing() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).open();

        seleniumMockSteps.assertWithWaiting(noRequest(hasSiteInfo(LISTING_LINK_SHOW)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрика «link-click», при переходе на публичный профиль перекупа на снипете листинга")
    public void shouldSeeLinkClickMetricFromListing() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).open();

        basePageSteps.onListingPage().salesList().get(0).resellerLink().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(LISTING_LINK_CLICK)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрика «link-show», при отображении ссылки на профиль перекупа на снипете листинга «Карусель»")
    public void shouldSeeLinkShowMetricFromListingCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.scrollDown(PXLS_TO_CAROUSEL_OFFER);

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(LISTING_LINK_SHOW)));
    }

    @Test
    @Ignore
    @Owner(ALEKS_IVANOV)
    @Issue("AUTORUFRONT-21850")
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет метрики «link-show», без отображения ссылки на профиль перекупа на снипете листинга «Карусель»")
    public void shouldSeeNoLinkClickMetricFromListingCarouselWithoutShowing() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        seleniumMockSteps.assertWithWaiting(noRequest(hasSiteInfo(LISTING_LINK_SHOW)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрика «link-click», при переходе на профиль перекупа на снипете листинга «Карусель»")
    public void shouldSeeLinkClickMetricFromListingCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).resellerLink().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(LISTING_LINK_CLICK)));
    }

}
