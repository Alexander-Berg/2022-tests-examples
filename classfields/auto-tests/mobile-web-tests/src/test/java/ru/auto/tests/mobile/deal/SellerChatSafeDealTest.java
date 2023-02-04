package ru.auto.tests.mobile.deal;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CHAT;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SAFE_DEAL;
import static ru.auto.tests.desktop.consts.QueryParams.GEO_ID;
import static ru.auto.tests.desktop.element.chat.SafeDealWidget.CANCEL_REQUEST;
import static ru.auto.tests.desktop.element.chat.SafeDealWidget.DETAILED;
import static ru.auto.tests.desktop.element.chat.SafeDealWidget.GO_TO_DEAL;
import static ru.auto.tests.desktop.element.chat.SafeDealWidget.SEND_REQUEST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Виджет безопасной сделки в чате продавца")
@Epic(AutoruFeatures.SAFE_DEAL)
@Feature("Виджет безопасной сделки в чате продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SellerChatSafeDealTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private LoginSteps loginSteps;

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());

        mockRule.newMock().with(
                "desktop/ChatRoomLight",
                "desktop/ChatRoomById",
                "desktop/ChatMessageWithTwoOwnerMessages",
                "desktop/SafeDealDealCreateSeller",
                "desktop/SafeDealDealCancelSellerFromChat",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(CHAT).path("/d934f15bb088fb83f7e4faa4531754c1/").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается пресет «Безопасная сделка» в чате с двумя сообщениями владельца")
    public void shouldSeeSafeDealPresetWithTwoOwnerMessages() {
        basePageSteps.onMainPage().chat().safeDealPreset().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается пресет «Безопасная сделка» в чате с одним сообщением владельца")
    public void shouldNotSeeSafeDealPresetWithOneOwnerMessage() {
        mockRule.overwriteStub(2, "desktop/ChatMessageWithOneOwnerMessage");

        basePageSteps.onMainPage().chat().safeDealPreset().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем виджет «Безопасная сделка» в чате")
    public void shouldCloseSafeDealWidget() {
        basePageSteps.onMainPage().chat().safeDealPreset().click();
        basePageSteps.onMainPage().chat().safeDealWidget().close().waitUntil(isDisplayed()).click();

        basePageSteps.onMainPage().chat().safeDealPreset().should(isDisplayed());
        basePageSteps.onMainPage().chat().safeDealWidget().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст виджета безопасной сделки в начальном состоянии")
    public void shouldSeeSafeDealWidgetText() {
        basePageSteps.onMainPage().chat().safeDealPreset().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onMainPage().chat().safeDealWidget().should(hasText(
                containsString(
                        "Безопасная сделка\n" +
                                "Предложите покупателю Безопасную сделку, а Авто.ру поможет" +
                                " с договором и передачей денег бесплатно\n" +
                                "Отправить запрос\n" +
                                "Подробнее\n" +
                                "Я соглашаюсь с условиями сервиса")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст виджета безопасной сделки после нажатия на «Отправить запрос»")
    public void shouldSeeSafeDealWidgetTextAfterSendRequest() {
        basePageSteps.onMainPage().chat().safeDealPreset().click();
        basePageSteps.onMainPage().chat().safeDealWidget().button(SEND_REQUEST).waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onMainPage().chat().safeDealWidget().should(hasText(
                containsString(
                        "Безопасная сделка\n" +
                                "Вы отправили запрос на безопасную сделку по Porsche 911 VIII (992).\n" +
                                "Теперь надо подождать, пока покупатель подтвердит запрос.\n" +
                                "Перейти к сделке\n" +
                                "Отменить запрос\n" +
                                "Я соглашаюсь с условиями сервиса")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Перейти к сделке»")
    public void shouldSeeGoToDealClick() {
        basePageSteps.onMainPage().chat().safeDealPreset().click();
        basePageSteps.onMainPage().chat().safeDealWidget().button(SEND_REQUEST).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().chat().safeDealWidget().button(GO_TO_DEAL).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.autoruProdURI().path(PROMO).path(SAFE_DEAL).ignoreParam(GEO_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Подробнее»")
    public void shouldSeeDetailedClick() {
        basePageSteps.onMainPage().chat().safeDealPreset().click();
        basePageSteps.onMainPage().chat().safeDealWidget().button(DETAILED).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.autoruProdURI().path(PROMO).path(SAFE_DEAL).ignoreParam(GEO_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нажимаем «Отменить запрос»")
    public void shouldCancelRequest() {
        basePageSteps.onMainPage().chat().safeDealPreset().click();
        basePageSteps.onMainPage().chat().safeDealWidget().button(SEND_REQUEST).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().chat().safeDealWidget().button(CANCEL_REQUEST).waitUntil(isDisplayed()).click();

        basePageSteps.onMainPage().chat().safeDealWidget().button(SEND_REQUEST).should(isDisplayed());
        basePageSteps.onMainPage().chat().safeDealWidget().button(GO_TO_DEAL).should(not(isDisplayed()));
        basePageSteps.onMainPage().chat().safeDealWidget().button(CANCEL_REQUEST).should(not(isDisplayed()));
    }

}
