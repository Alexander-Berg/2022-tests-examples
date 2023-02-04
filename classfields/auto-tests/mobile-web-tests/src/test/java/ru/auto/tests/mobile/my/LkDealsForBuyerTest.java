package ru.auto.tests.mobile.my;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.auto.tests.desktop.consts.Pages.DEALS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.page.lk.DealsPage.ABOUT_DEAL;
import static ru.auto.tests.desktop.page.lk.DealsPage.CANCEL_REQUEST;
import static ru.auto.tests.desktop.page.lk.DealsPage.GO_TO_DEAL;
import static ru.auto.tests.desktop.page.lk.DealsPage.RESUME_DEAL;
import static ru.auto.tests.desktop.page.lk.DealsPage.SEND_REQUEST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Безопасная сделка")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.SAFE_DEAL)
@Story("Сделки в ЛК под покупателем")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class LkDealsForBuyerTest {

    private static final String DEAL_ID_0 = "/2452bb38-a955-48bb-9d8a-83f538a510fb/";
    private static final String DEAL_ID_1 = "/3452bb38-a955-48bb-9d8a-83f538a510fb/";
    private static final String DEAL_ID_2 = "/d71a0533-d818-4960-bc9b-abf90a76f5c2/";

    private static final String NOTIFIER_TEXT = "Вы отправили запрос на безопасную сделку";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SafeDealDealListForBuyer").post();

        urlSteps.testing().path(MY).path(DEALS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Сделка завершена»")
    public void shouldSeeCompletedDeal() {
        basePageSteps.onLkDealPage().getDeal(0).waitUntil(isDisplayed()).should(hasText(
                "Porsche 911 Carrera 4S VIII (992)\nПокупка\nИмя продавца\nЧастное лицо\nСумма\n5 ₽\n" +
                        "Статус сделки\nСделка завершена\nПодробнее о сделке"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Заполнение паспортных данных» покупателя и продавца")
    public void shouldSeeDealInFillPassportDataState() {
        basePageSteps.onLkDealPage().getDeal(1).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nАктивна\nВаш статус\nЗаполнение паспортных данных\n" +
                        "Статус продавца\nЗаполнение паспортных данных\nПерейти к сделке"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Запрос на сделку» от продавца")
    public void shouldSeeDealInRequestFromSellerState() {
        basePageSteps.onLkDealPage().getDeal(2).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nЗапрос на сделку\nСвязаться\nОтклонить\nПодтвердить\n" +
                        "Я соглашаюсь с условиями сервиса"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Отклонена покупателем»")
    public void shouldSeeDealInCanceledByBuyerState() {
        basePageSteps.onLkDealPage().getDeal(3).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена покупателем\nВозобновить сделку"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Запрос на сделку» от покупателя")
    public void shouldSeeDealInRequestFromBuyerState() {
        basePageSteps.onLkDealPage().getDeal(4).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nЗапрос на сделку\nСвязаться\nОтменить запрос"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Отменена продавцом»")
    public void shouldSeeDealInCanceledBySellerState() {
        basePageSteps.onLkDealPage().getDeal(5).waitUntil(isDisplayed()).should(hasText(
                "Porsche 911 Carrera 4S VIII (992)\nПокупка\nИмя продавца\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтменена продавцом\nОтправить запрос еще раз"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Подробнее о сделке»")
    public void shouldClickDetailedAboutDeal() {
        mockRule.with("mobile/SafeDealDealGet2452bb38").update();

        basePageSteps.onLkDealPage().getDeal(0).button(ABOUT_DEAL).click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_0).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Перейти к сделке»")
    public void shouldClickGoToDeal() {
        mockRule.with("mobile/SafeDealDealGet3452bb38").update();

        basePageSteps.onLkDealPage().getDeal(1).button(GO_TO_DEAL).click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_1).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подтверждаем сделку по кнопке «Подтвердить»")
    public void shouldApproveDeal() {
        mockRule.with("desktop/SafeDealDealApproveBuyer").update();

        basePageSteps.onLkDealPage().getDeal(2).button("Подтвердить").click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_2).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку по кнопке «Отклонить» с причиной «Договорился с другим продавцом»")
    public void shouldDeclineDealWithAnotherBuyerReason() {
        mockRule.with("desktop/SafeDealDealUpdateBuyerDeclineAnotherSeller").update();

        basePageSteps.onLkDealPage().getDeal(2).button("Отклонить").click();
        basePageSteps.onLkDealPage().popup().reasonItem("Договорился с другим продавцом").click();

        basePageSteps.onLkDealPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.onLkDealPage().getDeal(2).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена покупателем\nВозобновить сделку"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку по кнопке «Отклонить» с причиной «Другое»")
    public void shouldDeclineDealWithAnotherReason() {
        mockRule.with("desktop/SafeDealDealUpdateBuyerDeclineAnotherReason").update();

        basePageSteps.onLkDealPage().getDeal(2).button("Отклонить").click();
        basePageSteps.onLkDealPage().popup().reasonItem("Другое").click();
        basePageSteps.onLkDealPage().popup().input().sendKeys("test test");
        basePageSteps.onLkDealPage().popup().button("Отправить").click();

        basePageSteps.onLkDealPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.onLkDealPage().getDeal(2).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена покупателем\nВозобновить сделку"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст в попапе возобновления сделки")
    public void shouldSeeTextInResumePopup() {
        basePageSteps.onLkDealPage().getDeal(3).button(RESUME_DEAL).click();

        basePageSteps.onLkDealPage().popup().should(hasText(
                "Предложите цену\nТорг уместен! Вы предлагаете продавцу свою цену, а он — соглашается " +
                        "или отказывается. Всё как обычно.\nОтправить запрос\nЯ соглашаюсь с условиями сервиса"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправляем запрос на сделку через «Возобновить сделку»")
    public void shouldResumeDealInResumePopup() {
        mockRule.with("desktop/SafeDealDealCreateWithOffer1113993894").update();

        basePageSteps.onLkDealPage().getDeal(3).button(RESUME_DEAL).click();
        basePageSteps.onLkDealPage().popup().button(SEND_REQUEST).click();

        basePageSteps.onLkDealPage().notifier().should(hasText(NOTIFIER_TEXT));
        basePageSteps.onLkDealPage().getDeal(3).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nЗапрос на сделку\nСвязаться\nОтменить запрос"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отмена запроса с причиной «Другое»")
    public void shouldCancelDealWithAnotherReason() {
        mockRule.with("desktop/SafeDealCancelRequestBuyerAnotherReason").update();

        basePageSteps.onLkDealPage().getDeal(4).button(CANCEL_REQUEST).click();
        basePageSteps.onLkDealPage().popup().reasonItem("Другое").click();
        basePageSteps.onLkDealPage().popup().input().sendKeys("test test");
        basePageSteps.onLkDealPage().popup().button("Отправить").click();

        basePageSteps.onLkDealPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.onLkDealPage().getDeal(4).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтменена покупателем\nВозобновить сделку"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку в попапе по кнопке «Отменить запрос»")
    public void shouldClickCancelRequestButtonInCancelPopup() {
        mockRule.with("desktop/SafeDealCancelRequestBuyerAnotherBuyer").update();

        basePageSteps.onLkDealPage().getDeal(4).button(CANCEL_REQUEST).click();
        basePageSteps.onLkDealPage().popup().reasonItem("Договорился с другим продавцом").click();

        basePageSteps.onLkDealPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.onLkDealPage().getDeal(4).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПокупка\nИмя продавца\nAndre\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтменена покупателем\nВозобновить сделку"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправляем запрос на сделку через «Отправить запрос еще раз»")
    public void shouldSendRequestOneMoreTime() {
        mockRule.with("desktop/SafeDealDealCreateWithOffer1113961017").update();

        basePageSteps.onLkDealPage().getDeal(5).button("Отправить запрос еще раз").click();
        basePageSteps.onLkDealPage().popup().button(SEND_REQUEST).click();

        basePageSteps.onLkDealPage().notifier().should(hasText(NOTIFIER_TEXT));
        basePageSteps.onLkDealPage().getDeal(5).should(hasText(
                "Porsche 911 Carrera 4S VIII (992)\nПокупка\nИмя продавца\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nЗапрос на сделку\nСвязаться\nОтменить запрос"));
    }

}
