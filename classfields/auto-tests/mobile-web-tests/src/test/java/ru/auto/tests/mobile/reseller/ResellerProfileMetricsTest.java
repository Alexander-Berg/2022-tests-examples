package ru.auto.tests.mobile.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.mountebank.http.predicates.PredicateType;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;

@DisplayName("Метрики на карточке оффера перекупа")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Метрики на публичном профиле")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class ResellerProfileMetricsTest {

    private static final String ID = getRandomOfferId();

    private static final String PUBLIC_PAGE_SHOW = "{\"all\":{\"reseller-public-page\":{\"show\":{}}}}";
    private static final String PHONE_SHOW = "{\"all\":{\"reseller-public-page\":{\"show-phone\":{\"user\":{\"listing\":{}}}}}}";
    private static final String CHAT_OPEN = "{\"all\":{\"reseller-public-page\":{\"chat_open\":{}}}}";

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
                sessionAuthUserStub(),
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody()),
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase()).setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample().setIdFirstOffer(ID)
                                        .setFiltersStatus(ACTIVE.toUpperCase()).build())
        ).create();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «reseller-public-page show» на публичном профиле перекупа")
    public void shouldSeeResellerPublicPageShow() {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PUBLIC_PAGE_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «show-phone» по «Показать телефон» со снипета на публичном профиле перекупа")
    public void shouldSeeResellerPublicShowPhone() {
        mockRule.setStubs(
                stub("cabinet/OfferCarsPhones").withPredicateType(PredicateType.EQUALS)
                        .withPath(format("%s/%s/phones", OFFER_CARS, ID))
        ).update();

        basePageSteps.onResellerPage().salesList().get(0).callButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PHONE_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «chat_open» по «Написать» на снипете")
    public void shouldSeeResellerPublicChatOpen() {
        basePageSteps.onResellerPage().salesList().get(0).sendMessage().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CHAT_OPEN)));
    }

}
