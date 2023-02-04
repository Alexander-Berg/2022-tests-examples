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
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE;
import static ru.auto.tests.desktop.element.Pager.SHOW_MORE;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Пейджинг на публичной странице перекупа")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature(PAGER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResellerProfilePagingTest {

    private static final String SECOND_PAGE = "2";
    private static final String THIRD_PAGE = "3";
    private static final int TOTAL_PAGE_COUNT = 4;
    private static final int FIRST_PAGE_OFFERS_COUNT = 10;
    private static final int SECOND_PAGE_OFFERS_COUNT = 5;

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
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setPageCount(TOTAL_PAGE_COUNT)
                                        .setOffers(FIRST_PAGE_OFFERS_COUNT).build()),
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody())
        ).create();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаемся на вторую страницу")
    public void shouldGoToSecondPage() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setPage(SECOND_PAGE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setPageCount(TOTAL_PAGE_COUNT)
                                        .setPage(Integer.parseInt(SECOND_PAGE))
                                        .setOffers(SECOND_PAGE_OFFERS_COUNT).build())
        );

        basePageSteps.onResellerPage().pager().page(SECOND_PAGE).click();

        basePageSteps.onResellerPage().salesList().should(hasSize(SECOND_PAGE_OFFERS_COUNT));
        basePageSteps.onResellerPage().pager().currentPage().should(hasText(SECOND_PAGE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаемся на следующую страницу")
    public void shouldGoToNextPage() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setPage(SECOND_PAGE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setPageCount(TOTAL_PAGE_COUNT)
                                        .setPage(Integer.parseInt(SECOND_PAGE))
                                        .setOffers(SECOND_PAGE_OFFERS_COUNT).build())
        );

        basePageSteps.onResellerPage().pager().next().click();

        basePageSteps.onResellerPage().salesList().should(hasSize(SECOND_PAGE_OFFERS_COUNT));
        basePageSteps.onResellerPage().pager().currentPage().should(hasText(SECOND_PAGE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаемся на предыдующую страницу")
    public void shouldGoToPrevPage() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setPage(THIRD_PAGE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setPageCount(TOTAL_PAGE_COUNT)
                                        .setPage(Integer.parseInt(THIRD_PAGE))
                                        .setOffers(FIRST_PAGE_OFFERS_COUNT).build())
        );

        urlSteps.addParam(PAGE, "3").open();
        basePageSteps.onResellerPage().pager().currentPage().waitUntil(hasText(THIRD_PAGE));

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setPage(SECOND_PAGE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setPageCount(TOTAL_PAGE_COUNT)
                                        .setPage(Integer.parseInt(SECOND_PAGE))
                                        .setOffers(SECOND_PAGE_OFFERS_COUNT).build())
        );

        basePageSteps.onResellerPage().pager().prev().click();

        basePageSteps.onResellerPage().salesList().should(hasSize(SECOND_PAGE_OFFERS_COUNT));
        basePageSteps.onResellerPage().pager().currentPage().should(hasText(SECOND_PAGE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Показать ещё»")
    public void shouldClickShowMoreOffers() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setPage(SECOND_PAGE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setPageCount(TOTAL_PAGE_COUNT)
                                        .setPage(Integer.parseInt(SECOND_PAGE))
                                        .setOffers(SECOND_PAGE_OFFERS_COUNT).build())
        );

        basePageSteps.onResellerPage().pager().button(SHOW_MORE).click();

        basePageSteps.onResellerPage().salesList().should(hasSize(FIRST_PAGE_OFFERS_COUNT + SECOND_PAGE_OFFERS_COUNT));
        basePageSteps.onResellerPage().pager().currentPage().should(hasText(SECOND_PAGE));
    }

}
