package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.consts.QueryParams.VIN_COMMENT_MODAL;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поп-ап «Комментируйте отчёт о своём автомобиле»")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinSaleOwnerCommentsPopupTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String POPUP_COOKIE = "hide_promo_about_comments";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUserOwner"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaidError")
        ).create();

        cookieSteps.deleteCookie(POPUP_COOKIE);
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID)
                .addParam(FORCE_POPUP, VIN_COMMENT_MODAL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeePopup() {
        basePageSteps.onCardPage().vinHistoryCommentsPopup().waitUntil(isDisplayed())
                .should(hasText("Комментируйте отчёт о своём автомобиле\nТеперь продавцы смогут оставлять свои " +
                        "комментарии и фото в отчёте об истории их автомобиля. Например, если есть данные, что машина " +
                        "была в ДТП, а по факту ущерб минимален, можно выложить снимки повреждений. Или сообщить, " +
                        "что штраф уже оплачен, а обременение снято.\nПонятно, спасибо"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePopup() {
        basePageSteps.onCardPage().vinHistoryCommentsPopup().closeIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().vinHistoryCommentsPopup().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(POPUP_COOKIE, "true");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Понятно, спасибо»")
    public void shouldClickOkButton() {
        basePageSteps.onCardPage().vinHistoryCommentsPopup().button("Понятно, спасибо").click();
        basePageSteps.onCardPage().vinHistoryCommentsPopup().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(POPUP_COOKIE, "true");
    }
}
