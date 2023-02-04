package ru.auto.tests.desktop.reseller;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Переход на профиль перекупа с листинга")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Провязки с листинга")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResellerListingTest {

    private static final String NAME = "Перекуп";

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                getSearchOffersUsedQuery())
                        .withResponseBody(
                                searchOffers(SEARCH_PROFESSIONAL_SELLER_OFFER)
                                        .setSellerName(NAME)
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

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на профиль перекупа с листинга")
    public void shouldGoToResellerProfileFromListing() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).open();

        basePageSteps.onListingPage().salesList().get(0).resellerLink().waitUntil(hasText(NAME)).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на профиль перекупа с листинга, тип листинга «Карусель»")
    public void shouldGoToResellerProfileFromListingCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().carouselSalesList().get(0).resellerLink().waitUntil(hasText(NAME)).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).shouldNotSeeDiff();
    }

}
