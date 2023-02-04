package ru.auto.tests.desktop.buyout;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.BUYOUT;
import static ru.auto.tests.desktop.consts.Pages.C2B_AUCTION;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.C2B_AUCTION_CAN_APPLY;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApply.c2bAuctionApplyResponse;
import static ru.auto.tests.desktop.page.BuyoutPage.ACCEPT_RULES;
import static ru.auto.tests.desktop.page.BuyoutPage.APPLICATION_STATUS;
import static ru.auto.tests.desktop.page.BuyoutPage.CHECK;
import static ru.auto.tests.desktop.page.BuyoutPage.CHECK_ANOTHER_AUTO;
import static ru.auto.tests.desktop.page.BuyoutPage.ESTIMATE;
import static ru.auto.tests.desktop.page.BuyoutPage.GOSNOMER;
import static ru.auto.tests.desktop.page.BuyoutPage.PHONE_NUMBER;
import static ru.auto.tests.desktop.page.BuyoutPage.POST;
import static ru.auto.tests.desktop.page.BuyoutPage.POST_FREE;
import static ru.auto.tests.desktop.page.BuyoutPage.RUNNING;
import static ru.auto.tests.desktop.page.BuyoutPage.SIGNUP;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Выкуп")
@Feature(BUYOUT)
@Story("Страница выкупа под зарегом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BuyoutTest {

    static private final String START_TEXT = "Оцените ваше авто\nУкажите гос. номер и пробег, и мы посчитаем предварительную стоимость вашего автомобиля\nГос номер или VIN\nПробег, км\nОценить";
    static private final String UNAPPLY_TEXT = "Не смогли оценить эту машину\nДля расчёта стоимости выкупа нам требуется больше данных.\nКак получить оценку?\nВведите параметры машины на форме размещения. Покажем предложение, если автомобиль подходит под эти условия:\nНе старше 2010 года\nПробег не выше 200 000 км\nПродаётся в Москве и МО\nПримерная стоимость от 500 000 до 4 000 000 ₽\nРазместить объявление\nОценить другой автомобиль";
    static private final String APPLY_TEXT = "2 345 915 — 2 779 700 ₽\nVOLVO XC60, 2018\n50 000 км\nОценить другой автомобиль\nБесплатный осмотр\nНаш эксперт приедет на осмотр в удобное для вас место, а после — сможем назвать точную цену\nМосква и Подмосковье\nВ пределах 20 км от МКАД в будни с 9:00 до 21:00.\nНомер телефона\nЗаписаться\nПринимаю правила и даю согласие на обработку персональных данных";
    static private final String SUCCESS_APPLICTAION_TEXT = "Получили вашу заявку\nПозвоним в течение часа с 9:00 до 18:00 в будни.\nОтветим на все вопросы и договоримся о месте и времени осмотра.\nОстались вопросы? 8 800 700-68-76\nПодробнее об Авто.ру Выкупе\nАвтомобиль\nVOLVO XC60, 2018\n50 000 км\nРекомендуем перед осмотром\nПомыть машину — это увеличит финальную цену\nВзять оригиналы документов: паспорт, ПТС, СТС\nНайти второй ключ\nПодготовить комплект сезонной резины, если есть\nПосмотреть статус заявки";
    static private final String USER_GOSNOMER = "Т350ВМ136";
    static private final String USER_RUNNING = "50000";
    static private final String NUMBER = "9111111111";

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(BUYOUT).open();
        basePageSteps.onBuyoutPage().floatingBuyoutButton().click();
        basePageSteps.onBuyoutPage().input(GOSNOMER, USER_GOSNOMER);
        basePageSteps.onBuyoutPage().input(RUNNING, USER_RUNNING);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Оценка невалидного авто под зарегом")
    public void shouldEstimateInvalidCarAuth() {
        mockRule.setStubs(
                stub().withPostDeepEquals(C2B_AUCTION_CAN_APPLY)
                        .withResponseBody(
                                c2bAuctionApplyResponse().setAuctionApply(false).getBody())
        ).update();

        basePageSteps.onBuyoutPage().button(ESTIMATE).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(UNAPPLY_TEXT));

        basePageSteps.onBuyoutPage().popup().button(POST).click();
        urlSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Оценка валидного авто под незарегом")
    public void shouldEstimateValidCarAuth() {
        mockRule.setStubs(
                stub("desktop/C2bAuctionCreateApplication"),
                stub().withPostDeepEquals(C2B_AUCTION_CAN_APPLY)
                        .withResponseBody(
                                c2bAuctionApplyResponse().setAuctionApply(true).getBody())
        ).update();

        basePageSteps.onBuyoutPage().button(ESTIMATE).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(APPLY_TEXT));

        basePageSteps.onBuyoutPage().popup().input(PHONE_NUMBER, NUMBER);
        basePageSteps.onBuyoutPage().popup().checkbox(ACCEPT_RULES).click();
        basePageSteps.onBuyoutPage().popup().button(SIGNUP).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(SUCCESS_APPLICTAION_TEXT));

        basePageSteps.onBuyoutPage().popup().button(APPLICATION_STATUS)
                .should(hasAttribute("href", urlSteps.testing().path(MY).path(C2B_AUCTION).toString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по кнопке «Оценить другой автомобиль»")
    public void shouldClickEstimateAnotherCarButtonAuth() {
        mockRule.setStubs(
                stub().withPostDeepEquals(C2B_AUCTION_CAN_APPLY)
                        .withResponseBody(
                                c2bAuctionApplyResponse().setAuctionApply(false).getBody())
        ).update();

        basePageSteps.onBuyoutPage().button(ESTIMATE).click();

        basePageSteps.onBuyoutPage().popup().button(CHECK_ANOTHER_AUTO).click();

        basePageSteps.onBuyoutPage().popup().should(hasText(START_TEXT));

    }
}
