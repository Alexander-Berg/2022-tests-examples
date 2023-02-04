package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Позиция в поиске. Легковые")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SearchPositionCarsTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersCarsActive").post();

        basePageSteps.setWideWindowSize();

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Попап с позицией в поиске")
    public void shouldSeePositionPopup() {
        basePageSteps.onLkResellerSalesPage().getSale(0).datesColumn().searchPosition().should(isDisplayed()).hover();
        basePageSteps.onLkResellerSalesPage().popup().should(hasText("Для того, чтобы быть выше в поиске, примените " +
                "поднятие.\nПоднять в поиске за 87 ₽\nПоказать в списке объявлений"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем в позицию в поиске на сниппете")
    public void shouldClickSearchPositionInSnippet() {
        basePageSteps.onLkResellerSalesPage().getSale(0).datesColumn().searchPosition().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(MOSKVA).path(CARS).path("/vaz/").path("/2121/").path(USED)
                .addParam("geo_radius", "200").addParam("scrollToPosition", "30")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем в позицию в поиске внутри попапа")
    public void shouldClickSearchPositionInPopup() {
        basePageSteps.onLkResellerSalesPage().getSale(0).datesColumn().searchPosition().should(isDisplayed()).hover();
        basePageSteps.onLkResellerSalesPage().popup().button("Показать в списке объявлений").click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(MOSKVA).path(CARS).path("/vaz/").path("/2121/").path(USED)
                .addParam("geo_radius", "200").addParam("scrollToPosition", "30")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем в Поднятие в поиске в попапе")
    public void shouldClickTopServiceInPopup() {
        mockRule.with("desktop-lk/reseller/BillingAutoruPaymentInitFresh",
                "desktop-lk/reseller/BillingAutoruPaymentProcess",
                "desktop-lk/BillingAutoruPayment").update();

        basePageSteps.onLkResellerSalesPage().getSale(0).datesColumn().searchPosition().should(isDisplayed()).hover();
        basePageSteps.onLkResellerSalesPage().popup().buttonContains("Поднять в поиске за 87").click();

        basePageSteps.onLkResellerSalesPage().switchToBillingFrame();
        basePageSteps.onLkResellerSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().billingPopup().header().waitUntil(hasText("Поднятие в поиске"));
        basePageSteps.onLkResellerSalesPage().billingPopup().priceHeader().waitUntil(hasText("237 \u20BD"));
    }
}
