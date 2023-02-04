package ru.auto.tests.desktop.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
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
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Редирект с закрытого профиля перекупа на листинг")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResellerPrivateProfileRedirectTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("error", "FORBIDDEN_REQUEST");
        errorResponse.addProperty("status", "ERROR");

        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody()),
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase())
                                        .setSort(DATE_DESC.getAlias()))
                        .withResponseBody(errorResponse)
                        .withStatusCode(403),
                stub("desktop/SearchCarsAll"),
                stub("desktop/SearchCarsBreadcrumbsEmpty")
        ).create();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редирект с закрытого профиля перекупа на листинг")
    public void shouldSeeRedirectFromPrivateProfileToListing() {
        basePageSteps.onListingPage().h1().should(hasText("Купить автомобиль\nВ Москве"));
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
    }

}
