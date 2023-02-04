package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Снятие с продажи")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class DeactivateSaleMotoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_MOTO_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.MOTO.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersMotoActive"),
                stub("desktop-lk/UserOffersMotoHide")
        ).create();

        urlSteps.testing().path(MY).path(MOTO).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        mockRule.overwriteStub(2, stub("desktop-lk/UserOffersMotoInactive"));

        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).deactivateButton().click();
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesNewPage().soldPopup().button("Снять с продажи").click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onLkSalesNewPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesNewPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().reviewsPromo().waitUntil(not(isDisplayed()));

        basePageSteps.onLkSalesNewPage().getSale(0).should(hasText("Снято с продажи\nVespa ET4, 2000\n100 км • " +
                "Инжектор • 49 (100 л.с.)\n50 000 ₽\nОбъявление готово к публикации\nОпубликовать объявление"));
    }

}
