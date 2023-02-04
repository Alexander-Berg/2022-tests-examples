package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SOCIAL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.consts.QueryParams.R;
import static ru.auto.tests.desktop.consts.QueryParams.TRUE;
import static ru.auto.tests.desktop.consts.QueryParams.YANDEX_AUTH_SUGGEST_NOTIFICATION;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.NOTIFICATION_YANDEX_AUTH_SUGGEST_SEEN;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап «Входите на Авто.ру одним кликом»")
@Epic(MAIN)
@Feature("Попап «Входите на Авто.ру одним кликом»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class YandexAuthSuggestNotificationTest {

    private static final String YANDEX_AUTH_TEXT = "Входите на Авто.ру одним кликом\nПрикрепите ваш аккаунт в Яндексе " +
            "к аккаунту на Авто.ру\nСвязать аккаунты";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty")
        ).create();

        urlSteps.testing().addParam(FORCE_POPUP, YANDEX_AUTH_SUGGEST_NOTIFICATION).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа «Входите на Авто.ру одним кликом»")
    public void shouldSeeYandexAuthPopupText() {
        basePageSteps.onMainPage().yandexAuthPopup().waitUntil(isDisplayed(), 10)
                .should(hasText(YANDEX_AUTH_TEXT));
        cookieSteps.shouldSeeCookieWithValue(NOTIFICATION_YANDEX_AUTH_SUGGEST_SEEN, TRUE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Связать аккаунты» в попапе «Входите на Авто.ру одним кликом»")
    public void shouldClickLinkAccountsYandexAuthPopup() {
        String currentUrl = urlSteps.getCurrentUrl();

        basePageSteps.onMainPage().yandexAuthPopup().button("Связать аккаунты")
                .waitUntil(isDisplayed(), 10).click();

        basePageSteps.switchToNextTab();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(SOCIAL).path("yandex").path(SLASH)
                .addParam(R, encode(currentUrl))
                .ignoreParam("_csrf_token").shouldNotSeeDiff();
    }

}
