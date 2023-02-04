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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
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
@GuiceModules(DesktopTestsModule.class)
public class ResellerProfileInfoTest {

    private static final String NAME = "Перекуп";

    private static final int CARS_COUNT = getRandomShortInt();
    private static final int MOTO_COUNT = getRandomShortInt();
    private static final int TRUCKS_COUNT = getRandomShortInt();

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
    @DisplayName("Отображение кол-ва «Автомобилей в наличии»")
    public void shouldSeeActiveOffersCount() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo()
                                        .setOffersCount("CARS", CARS_COUNT, getRandomShortInt())
                                        .setOffersCount("MOTO", MOTO_COUNT, getRandomShortInt())
                                        .setOffersCount("TRUCKS", TRUCKS_COUNT, getRandomShortInt())
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onResellerPage().activeOffers().should(hasText(
                format("%d в наличии", CARS_COUNT + MOTO_COUNT + TRUCKS_COUNT)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение кол-ва «Автомобилей продано»")
    public void shouldSeeInactiveOffersCount() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo()
                                        .setOffersCount("CARS", getRandomShortInt(), CARS_COUNT)
                                        .setOffersCount("MOTO", getRandomShortInt(), MOTO_COUNT)
                                        .setOffersCount("TRUCKS", getRandomShortInt(), TRUCKS_COUNT)
                                        .getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onResellerPage().inactiveOffers().should(hasText(format("%d продано",
                        CARS_COUNT + MOTO_COUNT + TRUCKS_COUNT)));
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
