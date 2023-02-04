package ru.auto.tests.desktop.main;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - фильтр по кредиту")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditCheckboxTest {

    private static final String CREDIT_PROMO_POPUP_COOKIE = "credit_filter_promo_popup_closed";
    private static final String TEXT = "Подбор кредита на спецусловиях" +
            "\nЗаполните заявку, которую сможете отправить в несколько банков и узнать решение онлайн." +
            "\nСумма кредита\nСрок кредита\n7 лет\nПервый взнос\nНе требуется\nПлатеж\n7 250 ₽ / мес.\n" +
            "Как работает «Авто.ру финанс»?\nФИО\nЭлектронная почта\nНомер телефона\nПодтвердить\n" +
            "Согласен на обработку данных: ООО Яндекс.Вертикали для оформления заявки; АО Объединенное " +
            "кредитное бюро для подготовки отчета ООО Яндекс.Вертикали; Банками партнерами; в " +
            "соответствии с условиями обработки данных.";
    private static final String PROMO_TEXT = "Подбор кредита на спецусловиях" +
            "\nЗаполните заявку, которую сможете отправить в несколько банков и узнать " +
            "решение онлайн.\nСумма кредита\nСрок кредита\n7 лет\nПервый взнос\nНе требуется\nПлатеж\n" +
            "9 650 ₽ / мес.\nРазыгрываем полное погашение автокредита\nФИО\nЭлектронная почта\nНомер телефона\nПодтвердить\n" +
            "Согласен на обработку данных: ООО Яндекс.Вертикали для оформления заявки; АО Объединенное кредитное бюро " +
            "для подготовки отчета ООО Яндекс.Вертикали; Банками партнерами; в соответствии с условиями обработки " +
            "данных. Отправляя заявку, Вы становитесь участником Конкурса \"Автомобиль за кредит\" в соответствии " +
            "с условиями.";

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SharkCreditProductCalculator")
        ).create();

        cookieSteps.deleteCookie(CREDIT_PROMO_POPUP_COOKIE);
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение подсказки про кредит при наведении")
    public void shouldSeeTooltipOnHover() {
        cookieSteps.setCookieForBaseDomain(CREDIT_PROMO_POPUP_COOKIE, "true");
        urlSteps.refresh();

        basePageSteps.onMainPage().checkbox("В кредит").hover();
        basePageSteps.onMainPage().popup().should(isDisplayed()).should(hasText("Подбирайте автомобиль по платежу в " +
                "месяц\nи оформляйте кредит прямо на Авто.ру\nУзнайте кредитный лимит"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должно отфильтровать объявления")
    public void shouldFilterOffers() {
        mockRule.setStubs(stub("desktop/SearchCarsMarkModelFiltersAllowedForCredit")).update();

        basePageSteps.onMainPage().checkbox("В кредит").click();
        basePageSteps.onMainPage().marksBlock().resultsButton().should(hasText("Показать 27 734 предложения")).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ON_CREDIT).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Чекбокса не должно быть когда переключаешься на Новые")
    public void shouldHideCreditCheckboxAtNew() {
        basePageSteps.onMainPage().marksBlock().switcher("Новые").click();

        basePageSteps.onMainPage().checkbox("В кредит").should(not(isDisplayed()));
        basePageSteps.onMainPage().popup().should(not(isDisplayed()));
    }
}
