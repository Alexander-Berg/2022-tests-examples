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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.offersExample;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Блок информации на публичной странице перекупа")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Блок информации о продавце")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ResellerProfileInfoTest {

    private static final String NAME = "Перекуп";

    private static final int CARS_ACTIVE_COUNT = getRandomShortInt();
    private static final int MOTO_ACTIVE_COUNT = getRandomShortInt();
    private static final int TRUCKS_ACTIVE_COUNT = getRandomShortInt();

    private static final int CARS_INACTIVE_COUNT = getRandomShortInt();
    private static final int MOTO_INACTIVE_COUNT = getRandomShortInt();
    private static final int TRUCKS_INACTIVE_COUNT = getRandomShortInt();

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
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase()).setSort(DATE_DESC.getAlias()))
                        .withResponseBody(
                                offersExample().setFiltersStatus(ACTIVE.toUpperCase()).build())
        );

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение H1 с именем продавца")
    public void shouldSeeSellerName() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().setAlias(NAME)
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onResellerPage().h1().should(hasText(format(
                "Профессиональный продавец\n%s", NAME)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение кол-ва объявлений")
    public void shouldSeeOffersCount() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo()
                                        .setOffersCount("CARS", CARS_ACTIVE_COUNT, CARS_INACTIVE_COUNT)
                                        .setOffersCount("MOTO", MOTO_ACTIVE_COUNT, MOTO_INACTIVE_COUNT)
                                        .setOffersCount("TRUCKS", TRUCKS_ACTIVE_COUNT, TRUCKS_INACTIVE_COUNT)
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onResellerPage().offersCount().should(hasText(
                format("%d в наличии / %d продано",
                        CARS_ACTIVE_COUNT + MOTO_ACTIVE_COUNT + TRUCKS_ACTIVE_COUNT,
                        CARS_INACTIVE_COUNT + MOTO_INACTIVE_COUNT + TRUCKS_INACTIVE_COUNT)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение лет на Авто.ру")
    public void shouldSeeSellerYears() {
        int yearsAgo = getRandomShortInt();

        String registrationDate = LocalDate.parse(LocalDate.now().toString()).minusYears(yearsAgo)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().setRegistrationDate(registrationDate)
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onResellerPage().years().should(hasText(
                matchesRegex(format("На Авто.ру %d (лет|год|года)", yearsAgo))));
    }

}
