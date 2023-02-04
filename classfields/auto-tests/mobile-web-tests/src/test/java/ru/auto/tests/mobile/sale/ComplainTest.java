package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.component.WithButton.SEND;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPLAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.page.CardPage.COMPLAIN_BUTTON;
import static ru.auto.tests.desktop.mobile.page.CardPage.NEXT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - жалобы")
@Feature(COMPLAIN)
@Story("Карточка оффера")
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ComplainTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String complainMock;

    @Parameterized.Parameter(3)
    public String complainText;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/OfferCarsComplaints", "Перекупщик или салон"},
                {CARS, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsComplaintsSold", "Продано"},

                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/OfferTrucksComplaints", "В другом городе"},

                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/OfferMotoComplaints", "Фото не соответствует"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock,
                complainMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().footer().hover();
        basePageSteps.onCardPage().mag().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отправка жалобы без описания")
    public void shouldComplain() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().button(COMPLAIN_BUTTON));
        basePageSteps.onCardPage().popup().checkbox(complainText).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().button(NEXT).click();
        basePageSteps.onCardPage().popup().button(SEND).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Спасибо, мы всё проверим"));
        basePageSteps.onCardPage().popup().waitUntil(not(isDisplayed()));
    }
}
