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
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.consts.QueryParams.GREAT_DEAL_MODAL;
import static ru.auto.tests.desktop.consts.QueryParams.SORT;
import static ru.auto.tests.desktop.consts.QueryParams.UTM_SOURCE;
import static ru.auto.tests.desktop.element.Popup.UNDERSTAND_THANKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.CLOSED;
import static ru.auto.tests.desktop.step.CookieSteps.GREAT_DEAL_POPUP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап «Сэкономьте на покупке автомобиля!»")
@Epic(MAIN)
@Feature("Попап «Сэкономьте на покупке автомобиля!»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GreatDealModalTest {

    private static final String GREAT_DEAL_TEXT = "Сэкономьте на покупке автомобиля!\nИщите объявления со значком " +
            "«Отличная цена». Эти машины продаются по цене ощутимо ниже рыночной.\nВыбрать машину\nПонятно, спасибо";

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

        urlSteps.testing().addParam(FORCE_POPUP, GREAT_DEAL_MODAL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа «Сэкономьте на покупке автомобиля!»")
    public void shouldSeeGreatDealPopupText() {
        basePageSteps.onMainPage().greatDealPopup().should(hasText(GREAT_DEAL_TEXT));
        cookieSteps.shouldSeeCookieWithValue(GREAT_DEAL_POPUP, CLOSED);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап «Сэкономьте на покупке автомобиля!»")
    public void shouldSeeGreatDealPopupClose() {
        basePageSteps.onMainPage().greatDealPopup().closeIcon().click();

        basePageSteps.onMainPage().greatDealPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап «Сэкономьте на покупке автомобиля!» по «Понятно, спасибо»")
    public void shouldSeeGreatDealPopupCloseByUnderstandButton() {
        basePageSteps.onMainPage().greatDealPopup().button(UNDERSTAND_THANKS).click();

        basePageSteps.onMainPage().greatDealPopup().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Выбрать машину» в попапе «Сэкономьте на покупке автомобиля!»")
    public void shouldClickChooseCarInGreatDeal() {
        basePageSteps.onMainPage().greatDealPopup().button("Выбрать машину").click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(SORT, "price_profitability-desc")
                .addParam(UTM_SOURCE, "greatdeal_popup")
                .fragment("priceRange").shouldNotSeeDiff();
    }

}
