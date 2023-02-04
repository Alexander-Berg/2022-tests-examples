package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.QueryParams.CAN_WE_CALL_YOU_MODAL;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.element.Popup.AGREE;
import static ru.auto.tests.desktop.element.Popup.NO_THANKS;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.CLOSED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап «Можем ли мы позвонить?»")
@Epic(MAIN)
@Feature("Попап «Можем ли мы позвонить?»")
@Story("Без авторизации")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class CanWeCallYouModalUnauthorizedTest {

    private static final String CAN_WE_CALL_YOU_TEXT = "Скидки и лучшие предложения\nНаши операторы готовы позвонить " +
            "и помочь вам найти лучшие скидки на интересующие вас модели. Вы согласны?\nСогласен\nНет, спасибо";

    private static final String PHONE_USAGE_POPUP_SHOW = "{\"cars\":{\"index\":{\"passport\":{\"phone_usage\":{\"pop-up\":{\"show\":{\"unauthorized\":{}}}}}}}}";
    private static final String PHONE_USAGE_POPUP_CLOSE = "{\"cars\":{\"index\":{\"passport\":{\"phone_usage\":{\"pop-up\":{\"close\":{\"unauthorized\":{}}}}}}}}";
    private static final String PHONE_USAGE_POPUP_ACCEPT = "{\"cars\":{\"index\":{\"passport\":{\"phone_usage\":{\"pop-up\":{\"accept\":{\"unauthorized\":{}}}}}}}}";
    private static final String PHONE_USAGE_POPUP_REJECT = "{\"cars\":{\"index\":{\"passport\":{\"phone_usage\":{\"pop-up\":{\"reject\":{\"unauthorized\":{}}}}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private SeleniumMockSteps browserMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty")
        ).create();

        urlSteps.testing().addParam(FORCE_POPUP, CAN_WE_CALL_YOU_MODAL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа «Можем ли мы позвонить?» неавторизованным")
    public void shouldSeeCanWeCallYouPopupText() {
        basePageSteps.onMainPage().canWeCallYouPopup().should(hasText(CAN_WE_CALL_YOU_TEXT));
        cookieSteps.shouldSeeCookieWithValue(CAN_WE_CALL_YOU_MODAL, CLOSED);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «phone_usage pop-up show unauthorized» на показ попапа «Можем ли мы позвонить?»")
    public void shouldSeeCanWeCallYouPopupShowMetric() {
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PHONE_USAGE_POPUP_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «phone_usage pop-up close unauthorized» на закрытие попапа «Можем ли мы позвонить?»")
    public void shouldSeeCanWeCallYouPopupCloseMetric() {
        basePageSteps.onMainPage().canWeCallYouPopup().closeIcon().click();

        basePageSteps.onMainPage().canWeCallYouPopup().should(not(isDisplayed()));
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PHONE_USAGE_POPUP_CLOSE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «phone_usage pop-up accept unauthorized» на «Согласен» в попапе «Можем ли мы позвонить?»")
    public void shouldSeeCanWeCallYouPopupAcceptMetric() {
        basePageSteps.onMainPage().canWeCallYouPopup().button(AGREE).click();

        basePageSteps.onMainPage().canWeCallYouPopup().should(not(isDisplayed()));
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PHONE_USAGE_POPUP_ACCEPT)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «phone_usage pop-up reject unauthorized» на «Нет, спасибо» в попапе «Можем ли мы позвонить?»")
    public void shouldSeeCanWeCallYouPopupRejectMetric() {
        basePageSteps.onMainPage().canWeCallYouPopup().button(NO_THANKS).click();

        basePageSteps.onMainPage().canWeCallYouPopup().should(not(isDisplayed()));
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PHONE_USAGE_POPUP_REJECT)));
    }

}
