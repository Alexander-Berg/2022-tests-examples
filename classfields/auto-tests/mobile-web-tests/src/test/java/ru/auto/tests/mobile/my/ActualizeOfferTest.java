package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mobile.page.LkPage.ACTUALIZE_POPUP_TEXT;
import static ru.auto.tests.desktop.mobile.page.LkPage.ACTUAL_MSG;
import static ru.auto.tests.desktop.mobile.page.LkPage.INFO_POPUP_TEXT;
import static ru.auto.tests.desktop.mobile.page.LkPage.NEED_UPDATE_MSG;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_MOTO_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_TRUCK_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.getDateDaysFromNow;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - актуализация объявления")
@Epic(LK)
@Feature(SALES)
@Story("Актуализация объявления")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ActualizeOfferTest {

    private static final int ACTUAL_PERIOD = -2;
    private static final int NON_ACTUAL_PERIOD = -5;
    private static final String ID = "1076842087-f1e84";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String userOfferPath;

    @Parameterized.Parameter(2)
    public String userOfferPathToMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, USER_OFFERS_CARS, USER_OFFER_CAR_EXAMPLE},
                {TRUCKS, USER_OFFERS_TRUCKS, USER_OFFER_TRUCK_EXAMPLE},
                {MOTO, USER_OFFERS_MOTO, USER_OFFER_MOTO_EXAMPLE}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/User"),
                stub("desktop/SessionAuthUser"),
                stub().withPostDeepEquals(format("%s/%s/actualize", userOfferPath, ID))
                        .withStatusSuccessResponse());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Актуализация объявления по кнопке «Актуально»")
    public void offerShouldBeActualizedDuringPeriod() {
        mockRule.setStubs(
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock).setActualizeDate(
                                        getDateDaysFromNow(ACTUAL_PERIOD))
                        ).build())
        ).create();

        urlSteps.testing().path(MY).path(category).open();
        basePageSteps.onLkPage().actualizationBlock().actualButton().waitUntil(not(exists()));
        basePageSteps.onLkPage().actualizationBlock().notActualButton().waitUntil(hasText(ACTUAL_MSG)).click();

        basePageSteps.onLkPage().popup().waitUntil(isDisplayed()).should(hasText(ACTUALIZE_POPUP_TEXT));

        basePageSteps.onLkPage().popup().closeIcon().click();

        basePageSteps.onLkPage().popup().should(not(isDisplayed()));
        basePageSteps.onLkPage().actualizationBlock().actualButton().should(isDisplayed()).should(hasText(ACTUAL_MSG));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Актуализация объявления по кнопке «Да, продаю»")
    public void offerShouldBeActualizedDuringAfterPeriod() {
        mockRule.setStubs(
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock).setActualizeDate(
                                        getDateDaysFromNow(NON_ACTUAL_PERIOD))
                        ).build())
        ).create();

        urlSteps.testing().path(MY).path(category).open();
        basePageSteps.onLkPage().actualizationBlock().actualButton().waitUntil(not(exists()));
        basePageSteps.onLkPage().actualizationBlock().notActualButton().waitUntil(hasText(NEED_UPDATE_MSG)).click();

        basePageSteps.onLkPage().popup().waitUntil(isDisplayed()).should(hasText(ACTUALIZE_POPUP_TEXT));

        basePageSteps.onLkPage().popup().closeIcon().click();

        basePageSteps.onLkPage().popup().should(not(isDisplayed()));
        basePageSteps.onLkPage().actualizationBlock().actualButton().should(isDisplayed()).should(hasText(ACTUAL_MSG));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «?»")
    public void shouldSeeActualizeInfoPopup() {
        mockRule.setStubs(
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock).setActualizeDate(
                                        getDateDaysFromNow(NON_ACTUAL_PERIOD))
                        ).build())
        ).create();

        urlSteps.testing().path(MY).path(category).open();
        basePageSteps.onLkPage().actualizationBlock().infoButton().click();

        basePageSteps.onLkPage().popup().waitUntil(isDisplayed()).should(hasText(INFO_POPUP_TEXT));
    }

}
