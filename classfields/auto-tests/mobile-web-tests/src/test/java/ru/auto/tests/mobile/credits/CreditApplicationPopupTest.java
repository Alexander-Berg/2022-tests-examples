package ru.auto.tests.mobile.credits;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.MATCHES;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.DRAFT;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockCreditApplicationCreateRequest.creditApplicationCreateRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап короткой заявки кредита")
@Epic(AutoruFeatures.CREDITS)
@Feature("Попап короткой заявки кредита")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CreditApplicationPopupTest {

    private static final String POPUP_TEXT = "Подбор кредита на спецусловиях\nПервый взнос\n0 ₽\nПлатеж\n10 150 ₽ " +
            "/ мес.\nСумма кредита\nСрок кредита\n7 лет\nФИО\nЭлектронная почта\nНомер телефона\nПодтвердить\n" +
            "Согласен на обработку данных: ООО Яндекс.Вертикали для оформления заявки; АО Объединенное кредитное " +
            "бюро и АО Яндекс Банк для подготовки отчета ООО Яндекс.Вертикали; Банками партнерами; в соответствии " +
            "с условиями обработки данных.";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("mobile/SearchCarsOneUserSaleForCredit"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SharkCreditProductList"),
                stub("desktop/SharkCreditApplicationActiveWithOffersEmptyAndDraft"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfiles"),
                stub("desktop/SharkCreditApplicationUpdate"),
                stub("desktop/SharkBankList"),
                stub("desktop/SharkCreditProductCalculator")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
        basePageSteps.onListingPage().getSale(0).creditPrice().click();
        basePageSteps.onListingPage().creditApplicationPopup().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст попапа короткой заявки кредита")
    public void shouldSeePopupText() {
        basePageSteps.onListingPage().creditApplicationPopup().should(hasText(POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем сумму кредита через инпут")
    public void shouldChangePriceInput() {
        int maxAmount = 600000;

        basePageSteps.onListingPage().creditApplicationPopup().amountBlock().priceEditIcon().click();
        basePageSteps.onListingPage().creditApplicationPopup().amountBlock().clearInput();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        mockRule.setStubs(
                stub("desktop/SharkCreditApplicationCreate")
                        .withPredicateType(MATCHES)
                        .withRequestBody(
                                creditApplicationCreateRequest()
                                        .setMaxAmount(maxAmount).getBody())
        ).update();

        basePageSteps.onListingPage().creditApplicationPopup().amountBlock().input().sendKeys(String.valueOf(maxAmount));
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем сумму кредита через слайдер")
    public void shouldChangePriceSlider() {
        int middleSliderAmount = 275000;

        basePageSteps.onListingPage().creditApplicationPopup().amountBlock().slider().click();

        mockRule.setStubs(
                stub("desktop/SharkCreditApplicationCreate")
                        .withPredicateType(MATCHES)
                        .withRequestBody(
                                creditApplicationCreateRequest()
                                        .setMaxAmount(middleSliderAmount).getBody())
        ).update();

        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем срок кредита через слайдер")
    public void shouldChangeTermInput() {
        int middleSliderTermMonths = 48;

        basePageSteps.onListingPage().creditApplicationPopup().termBlock().value().waitUntil(hasText("7 лет"));

        mockRule.setStubs(
                stub("desktop/SharkCreditApplicationCreate")
                        .withPredicateType(MATCHES)
                        .withRequestBody(
                                creditApplicationCreateRequest()
                                        .setTermMonths(middleSliderTermMonths).getBody())
        ).update();

        basePageSteps.onListingPage().creditApplicationPopup().termBlock().slider().click();
        basePageSteps.onListingPage().creditApplicationPopup().termBlock().value().waitUntil(hasText("4 года"));
        basePageSteps.onListingPage().creditApplicationPopup().button("Подтвердить").click();

        urlSteps.testing().path(MY).path(CREDITS).path(DRAFT).shouldNotSeeDiff();
    }

}
