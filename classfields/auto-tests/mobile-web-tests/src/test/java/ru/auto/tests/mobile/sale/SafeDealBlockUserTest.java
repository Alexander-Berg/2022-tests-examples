package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SAFE_DEAL;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок безопасной сделки")
@Feature(SALES)
@Story(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SafeDealBlockUserTest {

    private static final String PATH = "/kia/optima/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserWithAllowedSafeDeal",
                "desktop/SafeDealDealCreate",
                "desktop/SafeDealDealListForBuyer").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(PATH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Скролл к блоку сделки")
    public void shouldScrollToDeal() {
        basePageSteps.onCardPage().cardActions().dealButton().click();
        waitSomething(1, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к отчётам", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока безопасной сделки под авторизованным пользователем")
    public void shouldSeeDealBlock() {
        basePageSteps.onCardPage().dealBlock().waitUntil(isDisplayed()).should(hasText("Купите этот автомобиль с " +
                "Безопасной сделкой\nАвто.ру бесплатно поможет с договором и передачей денег.\nПодробнее\nНачать " +
                "сделку\nЯ соглашаюсь с условиями сервиса"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Начать сделку»")
    public void shouldClickBeginningButton() {
        createSafeDeal();
        basePageSteps.onCardPage().existingDealBanner().button("Перейти к сделкам").click();
        urlSteps.testing().path(MY).path(DEALS).shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Подробнее»")
    public void shouldClickMoreButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().dealBlock().button("Подробнее")
                .waitUntil(isDisplayed()));
        urlSteps.switchToNextTab();
        urlSteps.testing().path(PROMO).path(SAFE_DEAL).shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Отменить заявку»")
    public void shouldClickCancelButton() {
        mockRule.with("desktop/SafeDealDealUpdateBuyerCancelRequest").update();
        createSafeDeal();
        basePageSteps.onCardPage().existingDealBanner().button("Отменить").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Почему решили отменить?" +
                "\nДоговорился с другим продавцом\nНе стал переводить деньги\nПродавец бездействует\nПередумал покупать" +
                "\nДругое"));
        basePageSteps.onCardPage().popup().reasonItem("Договорился с другим продавцом").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().dealBlock().button("Начать сделку")
                .waitUntil(isDisplayed()));
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Отменить заявку» с причиной «Другое»")
    public void shouldClickCancelButtonWithAnotherReason() {
        mockRule.with("desktop/SafeDealCancelRequestBuyerAnotherReason").update();
        createSafeDeal();
        basePageSteps.onCardPage().existingDealBanner().button("Отменить").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Почему решили отменить?" +
                "\nДоговорился с другим продавцом\nНе стал переводить деньги\nПродавец бездействует\nПередумал покупать" +
                "\nДругое"));
        basePageSteps.onCardPage().popup().reasonItem("Другое").click();
        basePageSteps.onDealPage().popup().input().sendKeys("test test");
        basePageSteps.onDealPage().popup().button("Отправить").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().dealBlock().button("Начать сделку")
                .waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопки «Безопасная сделка» в действиях с объявлением не должно быть")
    public void shouldNotSeeSafeDealButtonInCardActions() {
        mockRule.overwriteStub(1, "desktop/OfferCarsUsedUser");
        urlSteps.refresh();

        basePageSteps.onCardPage().cardActions().dealButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блока «Безопасная сделка» в заголовке объявления не должно быть")
    public void shouldNotSeeDealBlock() {
        mockRule.overwriteStub(1, "desktop/OfferCarsUsedUser");
        urlSteps.refresh();

        basePageSteps.onCardPage().dealBlock().should(not(isDisplayed()));
    }

    @Step("Отправка запроса на безопасную сделку")
    public void createSafeDeal() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().dealBlock().button("Начать сделку")
                .waitUntil(isDisplayed()));
        basePageSteps.onCardPage().popup().button("Отправить запрос").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Вы отправили запрос на безопасную сделку"));
        basePageSteps.onCardPage().existingDealBanner().waitUntil(isDisplayed()).should(hasText("Подождите, пока" +
                " продавец примет ваш запрос на сделку\nОтменитьПерейти к сделкам"));
    }
}
