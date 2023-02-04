package ru.auto.tests.desktop.lk.sales;

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
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffer.getDateDaysFromNow;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@DisplayName("Актуальность объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActualizeCarsTest {

    private static final String IS_ACTUAL = "Актуально";
    private static final String IS_NOT_ACTUAL = "Да, продаю";
    private static final String ID = "1076842087-f1e84";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub().withPostDeepEquals(format("%s/%s/actualize", USER_OFFERS_CARS, ID))
                        .withStatusSuccessResponse(),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        );
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим статус «Актуально» в течение первых трёх суток")
    public void shouldSeeActualStatus() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                car().setActualizeDate(getDateDaysFromNow(-2))
                        ).build())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();

        basePageSteps.onLkSalesPage().getSale(0).actualizeButton().should(hasText(IS_ACTUAL));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Видим синюю кнопку «Да, продаю» после трёх дней")
    public void shouldSeeYesActualButton() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                car().setActualizeDate(getDateDaysFromNow(-4))
                        ).build())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();

        basePageSteps.onLkSalesPage().getSale(0).actualizeButton().should(hasText(IS_NOT_ACTUAL));
        basePageSteps.onLkSalesPage().getSale(0).actualizeButton().waitUntil(isDisplayed()).click();

        basePageSteps.onLkSalesPage().getSale(0).actualizeButton().waitUntil(hasText(IS_ACTUAL));
    }

}
