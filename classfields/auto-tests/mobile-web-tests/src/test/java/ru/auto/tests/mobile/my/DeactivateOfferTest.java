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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Notifications.STATUS_CHANGED;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mobile.element.lk.SalesItem.ACTIVATE_BUTTON;
import static ru.auto.tests.desktop.mobile.element.lk.SalesItem.DEACTIVATE_BUTTON;
import static ru.auto.tests.desktop.mobile.element.lk.SalesItem.OFFER_DEACTIVATED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - снятие с продажи объявления легковых, мото, и коммерческого т/с")
@Epic(LK)
@Feature(SALES)
@Story("Деактивация объявления")
@GuiceModules(MobileTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeactivateOfferTest {

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
    public String mockHide;

    @Parameterized.Parameter(1)
    public String mockActive;

    @Parameterized.Parameter(2)
    public String category;

    @Parameterized.Parameter(3)
    public String mockInactive;

    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"mobile/UserOffersCarsHide", "desktop/UserOffersCarsActive", CARS, "desktop/UserOffersCarsInactive"},
                {"mobile/UserOffersMotoHide", "desktop/UserOffersMotoActive", MOTO, "desktop/UserOffersMotoInactive"},
                {"mobile/UserOffersTrucksHide", "desktop/UserOffersTrucksActive", TRUCKS, "desktop/UserOffersTrucksInactive"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/User"),
                stub("desktop/SessionAuthUser"),
                stub(mockActive),
                stub(mockHide)).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEXANDERREX)
    @DisplayName("Клик по кнопке «Снять с продажи»")
    public void shouldClickDeactivateButton() {
        urlSteps.testing().path(MY).path(category).open();

        mockRule.overwriteStub(1, stub(mockInactive));
        basePageSteps.onLkPage().getSale(0).button(DEACTIVATE_BUTTON).waitUntil(isDisplayed()).click();
        basePageSteps.onLkPage().reasonToRemoveFromSalePopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkPage().reasonToRemoveFromSalePopup()
                .button(DEACTIVATE_BUTTON).waitUntil(isDisplayed()).click();
        basePageSteps.onLkPage().notifier(STATUS_CHANGED).waitUntil(isDisplayed());
        basePageSteps.onLkPage().reasonToRemoveFromSalePopup().waitUntil(not(isDisplayed()));

        basePageSteps.onLkPage().getSale(0).button(DEACTIVATE_BUTTON).should(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button(ACTIVATE_BUTTON).should(isDisplayed());
        basePageSteps.onLkPage().getSale(0).status().waitUntil(isDisplayed()).should(hasText(OFFER_DEACTIVATED));
    }
}
