package ru.auto.tests.desktop.reseller;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.element.SortBar.SortBy;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.NAME_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.YEAR_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.YEAR_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.page.ResellerPage.SORT;

@DisplayName("Применяем сортировку на публичной странице перекупа")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature(SORT)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ResellerProfileSortTest {

    private static final int COUNT_BEFORE_SORT = 10;
    private static final int COUNT_AFTER_SORT = 5;

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

    @Parameterized.Parameter
    public SortBy sortBy;

    @Parameterized.Parameter(1)
    public String sortName;

    @Parameterized.Parameter(2)
    public String sortDesc;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {PRICE_ASC, "price", "false"},
                {PRICE_DESC, "price", "true"},
                {YEAR_DESC, "year", "true"},
                {YEAR_ASC, "year", "false"},
                {NAME_ASC, "name", "false"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase()).setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                resellerOffersExample().setOffers(COUNT_BEFORE_SORT).build()),
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody())
        ).create();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем сортировку с разными типами")
    public void shouldApplySort() {
        basePageSteps.onResellerPage().salesList().waitUntil(hasSize(COUNT_BEFORE_SORT));

        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setSort(sortBy.getAlias()))
                        .withResponseBody(
                                resellerOffersExample().setOffers(COUNT_AFTER_SORT)
                                        .setSorting(sortName, sortDesc)
                                        .build())
        );

        basePageSteps.onResellerPage().selectItem(SORT, sortBy.getName());

        basePageSteps.onResellerPage().salesList().should(hasSize(COUNT_AFTER_SORT));
        urlSteps.addParam(QueryParams.SORT, sortBy.getAlias()).shouldNotSeeDiff();
    }

}
