package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.AUTH;
import static ru.yandex.realty.element.base.HeaderMain.LOGIN;

@DisplayName("Главная. Авторизация. Кнопка «Войти»")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AuthBasePageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Войти»")
    public void shouldSeePassportPopup() {
        urlSteps.testing().open();
        basePageSteps.onBasePage().headerMain().link(LOGIN).click();
        basePageSteps.refreshUntil(() -> basePageSteps.onBasePage().domikPopup(), isDisplayed());
        basePageSteps.onBasePage().domikPopup().should(isDisplayed());
    }
}
