package ru.auto.tests.mobile.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.AUTORU_TCS;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.QueryParams.ONLY_CONTENT;
import static ru.auto.tests.desktop.consts.Urls.DOWNLOAD_APP_URL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо с призывом обновить приложение при открытии в вебвью")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class UpdateAppPromoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(Pages.PROMO).path(AUTORU_TCS).addParam(ONLY_CONTENT, "true").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("В WebView отображается страница «Обнови приложение»")
    public void shouldSeeUpdateAppPromo() {
        basePageSteps.onPromoPage().creditForceAppUpdatePromo().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Кнопка «Обнови приложение» содержит ссылку на app")
    public void shouldContainsAppUrl() {
        basePageSteps.onPromoPage().button("Обновить")
                .should(hasAttribute("href", DOWNLOAD_APP_URL));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Без WebView страница «Обнови приложение» не отображается")
    public void shouldNotSeeUpdateAppPromo() {
        cookieSteps.deleteCookie("webview");
        urlSteps.testing().path(Pages.PROMO).path(AUTORU_TCS).open();

        urlSteps.testing().path(Pages.PROMO).path(FINANCE).shouldNotSeeDiff();
        basePageSteps.onPromoPage().button("Оформить кредит").should(isDisplayed());
    }
}
