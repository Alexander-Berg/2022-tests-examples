package ru.auto.tests.desktop.delivery;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Regions.YAROSLAVL_GEO_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок доставки на карточке объявления - кнопка «Подробнее по телефону»")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeliveryPhoneButtonTest {

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
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedDealerWithDelivery",
                "desktop/OfferCarsPhones").post();

        cookieSteps.setCookieForBaseDomain("gids", YAROSLAVL_GEO_ID);
        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие поп-апа")
    public void shouldClickPhoneButton() {
        basePageSteps.onCardPage().deliveryInfo().button(" Подробнее\u00a0по\u00a0телефону").click();
        basePageSteps.onCardPage().contactsPopup().waitUntil(isDisplayed());
    }
}