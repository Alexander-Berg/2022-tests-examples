package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка услуги «Спецпредложение»")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasBuySpecialTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String salesMock;

    @Parameterized.Parameter(2)
    public String offerIdMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop-lk/UserOffersCarsActive", "desktop-lk/reseller/UserOffersCarsIdWithAllServices"},
                {MOTO, "desktop-lk/UserOffersMotoActive", "desktop-lk/reseller/UserOffersMotoIdWithAllServices"},
                {TRUCKS, "desktop-lk/UserOffersTrucksActive", "desktop-lk/reseller/UserOffersTrucksIdWithAllServices"}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                salesMock,
                offerIdMock,
                "desktop-lk/BillingAutoruPaymentInitSpecial",
                "desktop-lk/reseller/BillingAutoruPaymentProcess",
                "desktop-lk/BillingAutoruPayment").post();

        urlSteps.testing().path(MY).path(RESELLER).path(category).open();
    }

    @Test
    @Ignore //TODO: Удалить или переделать, потому что вместо Спецпредложений теперь Показ в историях
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() {
        basePageSteps.onLkResellerSalesPage().getSale(0).vas("Спецпредложение").should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().switchToBillingFrame();
        basePageSteps.onLkResellerSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().billingPopup().checkbox("Запомнить карту").click();
        basePageSteps.onLkResellerSalesPage().billingPopup().header().waitUntil(hasText("Спецпредложение"));
        basePageSteps.onLkResellerSalesPage().billingPopup().priceHeader().waitUntil(hasText("599 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onLkResellerSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Опция успешно активирована"));
        basePageSteps.onLkResellerSalesPage().billingPopupCloseButton().click();
        basePageSteps.onLkResellerSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onLkResellerSalesPage().getSale(0).vas("Спецпредложение")
                .should(hasText("×5 просмотров\nСпецпредложение • 3 дня\nВключено"));
    }
}
