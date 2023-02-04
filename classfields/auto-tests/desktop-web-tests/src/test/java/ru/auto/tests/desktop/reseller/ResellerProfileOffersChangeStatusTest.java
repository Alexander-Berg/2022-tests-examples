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
import ru.auto.tests.desktop.page.ResellerPage;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.INACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.page.ResellerPage.IN_STOCK;
import static ru.auto.tests.desktop.page.ResellerPage.SOLD;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

@DisplayName("Смена статуса офферов в листинге")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Смена статуса офферов в листинге")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResellerProfileOffersChangeStatusTest {

    private static final int ACTIVE_COUNT = 10;
    private static final int INACTIVE_COUNT = 5;

    private static final String BUTTON_CHECKED = "Button_checked";

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
                                        .setFiltersStatus(ACTIVE.toUpperCase())
                                        .setOffers(ACTIVE_COUNT)
                                        .build()),
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody())
        ).create();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаемся на проданные офферы")
    public void shouldGoToSoldOffers() {
        basePageSteps.onResellerPage().salesList().waitUntil(hasSize(ACTIVE_COUNT));
        waitUntilInStockButtonChecked();

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(INACTIVE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setFiltersStatus(INACTIVE)
                                        .setOffers(INACTIVE_COUNT)
                                        .build())
        );

        basePageSteps.onResellerPage().status(ResellerPage.SOLD).click();

        basePageSteps.onResellerPage().salesList().should(hasSize(INACTIVE_COUNT));
        waitUntilSoldButtonChecked();
        urlSteps.addParam(STATUS, INACTIVE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаемся на активные офферы")
    public void shouldGoToActiveOffers() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(INACTIVE)
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setFiltersStatus(INACTIVE)
                                        .setOffers(INACTIVE_COUNT)
                                        .build())
        );

        urlSteps.addParam(STATUS, INACTIVE).open();
        waitUntilSoldButtonChecked();
        basePageSteps.onResellerPage().salesList().waitUntil(hasSize(INACTIVE_COUNT));

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample()
                                        .setFiltersStatus(ACTIVE.toUpperCase())
                                        .setOffers(ACTIVE_COUNT).build())
        );

        basePageSteps.onResellerPage().status(IN_STOCK).click();

        basePageSteps.onResellerPage().salesList().should(hasSize(ACTIVE_COUNT));
        waitUntilInStockButtonChecked();
        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).shouldNotSeeDiff();
    }

    private void waitUntilInStockButtonChecked() {
        basePageSteps.onResellerPage().status(IN_STOCK).waitUntil(hasClass(containsString(BUTTON_CHECKED)));
        basePageSteps.onResellerPage().status(SOLD).waitUntil(not(hasClass(containsString(BUTTON_CHECKED))));
    }

    private void waitUntilSoldButtonChecked() {
        basePageSteps.onResellerPage().status(SOLD).waitUntil(hasClass(containsString(BUTTON_CHECKED)));
        basePageSteps.onResellerPage().status(IN_STOCK).waitUntil(not(hasClass(containsString(BUTTON_CHECKED))));
    }

}
