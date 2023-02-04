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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.AUTO_RU_EXCLUSIVE_MODAL;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.UTM_SOURCE;
import static ru.auto.tests.desktop.element.Popup.UNDERSTAND_THANKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.AUTORU_EXCLUSIVE_POPUP;
import static ru.auto.tests.desktop.step.CookieSteps.CLOSED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап «Только на Авто.ру»")
@Epic(MAIN)
@Feature("Попап «Только на Авто.ру»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AutoRuExclusiveModalTest {

    private static final String AUTORU_EXCLUSIVE_TEXT = "Эти 40 000 машин есть только у нас\nНа Авто.ру — самая " +
            "большая база объявлений в Москве. А 40 000 из них — не найти нигде больше. Мы сделали фильтр по ним.\n" +
            "Посмотреть объявления\nПонятно, спасибо";

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

        urlSteps.testing().addParam(FORCE_POPUP, AUTO_RU_EXCLUSIVE_MODAL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа «Только на Авто.ру»")
    public void shouldSeeAutoRuExclusivePopupText() {
        basePageSteps.onMainPage().autoruExclusivePopup().should(hasText(AUTORU_EXCLUSIVE_TEXT));
        cookieSteps.shouldSeeCookieWithValue(AUTORU_EXCLUSIVE_POPUP, CLOSED);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап «Только на Авто.ру»")
    public void shouldSeeAutoruExclusivePopupClose() {
        basePageSteps.onMainPage().autoruExclusivePopup().closeIcon().click();

        basePageSteps.onMainPage().autoruExclusivePopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап «Только на Авто.ру» по «Понятно, спасибо»")
    public void shouldSeeAutoruExclusivePopupCloseByUnderstandButton() {
        basePageSteps.onMainPage().autoruExclusivePopup().button(UNDERSTAND_THANKS).click();

        basePageSteps.onMainPage().autoruExclusivePopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Посмотреть объявления» в попапе «Только на Авто.ру»")
    public void shouldClickChooseCarInAutoruExclusive() {
        basePageSteps.onMainPage().autoruExclusivePopup().button("Посмотреть объявления").click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(FROM, "autoru_exclusive_popup")
                .addParam(UTM_SOURCE, "autoru_exclusive_popup")
                .fragment("priceRange").shouldNotSeeDiff();
    }

}
