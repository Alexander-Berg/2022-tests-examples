package ru.auto.tests.desktop.lk.deals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.auto.tests.desktop.consts.Pages.DEALS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.lk.DealsPage.ABOUT_DEAL;
import static ru.auto.tests.desktop.page.lk.DealsPage.CANCEL_REQUEST;
import static ru.auto.tests.desktop.page.lk.DealsPage.GO_TO_DEAL;
import static ru.auto.tests.desktop.page.lk.DealsPage.GO_TO_OFFER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Безопасная сделка")
@Epic(AutoruFeatures.SAFE_DEAL)
@Feature("Сделки в ЛК под продавцом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealsForSellerTest {

    private static final String DEAL_ID_0 = "/2452bb38-a955-48bb-9d8a-83f538a510fb/";
    private static final String DEAL_ID_1 = "/3452bb38-a955-48bb-9d8a-83f538a510fb/";
    private static final String DEAL_ID_4 = "/d71a0533-d818-4960-bc9b-abf90a76f5c4/";

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
                "desktop/SafeDealDealListForSeller").post();

        urlSteps.testing().path(MY).path(DEALS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Сделка завершена»")
    public void shouldSeeCompletedDeal() {
        basePageSteps.onLkDealsPage().getDeal(0).waitUntil(isDisplayed()).should(hasText(
                "Porsche 911 Carrera 4S VIII (992)\nПродажа\nИмя покупателя\nIv Alex\nСумма\n5 ₽\n" +
                        "Статус сделки\nСделка завершена\nПодробнее о сделке"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Заполнение паспортных данных» покупателя и продавца")
    public void shouldSeeDealInFillPassportDataState() {
        basePageSteps.onLkDealsPage().getDeal(1).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nАктивна\nВаш статус\nЗаполнение паспортных данных\n" +
                        "Статус покупателя\nЗаполнение паспортных данных\nПерейти к сделке"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Запрос на сделку» от продавца")
    public void shouldSeeDealInRequestFromSellerState() {
        basePageSteps.onLkDealsPage().getDeal(2).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nЗапрос на сделку\nСвязаться\nОтменить запрос"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Отклонена покупателем»")
    public void shouldSeeDealInCanceledByBuyerState() {
        basePageSteps.onLkDealsPage().getDeal(3).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена покупателем\nПерейти к объявлению"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Запрос на сделку» от покупателя")
    public void shouldSeeDealInRequestFromBuyerState() {
        basePageSteps.onLkDealsPage().getDeal(4).waitUntil(isDisplayed()).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nЗапрос на сделку\nСвязаться\nОтклонить\nПодтвердить\n" +
                        "Я соглашаюсь с условиями сервиса"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сделка в состоянии - «Отменена продавцом»")
    public void shouldSeeDealInCanceledBySellerState() {
        basePageSteps.onLkDealsPage().getDeal(5).waitUntil(isDisplayed()).should(hasText(
                "Porsche 911 Carrera 4S VIII (992)\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтменена продавцом\nПерейти к объявлению"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Подробнее о сделке»")
    public void shouldClickDetailedAboutDeal() {
        basePageSteps.onLkDealsPage().getDeal(0).button(ABOUT_DEAL).click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_0).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Перейти к сделке»")
    public void shouldClickGoToDeal() {
        basePageSteps.onLkDealsPage().getDeal(1).button(GO_TO_DEAL).click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_1).shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отмена запроса с причиной «Другое»")
    public void shouldCancelDealWithAnotherReason() {
        mockRule.with("desktop/SafeDealCancelRequestSellerAnotherReason").update();

        basePageSteps.onLkDealsPage().getDeal(2).button(CANCEL_REQUEST).click();
        basePageSteps.onLkDealsPage().popup().selectItem("Выберите причину", "Другое");
        basePageSteps.onLkDealsPage().popup().input("Опишите причину", "test test");
        basePageSteps.onLkDealsPage().popup().button("Отправить").click();

        basePageSteps.onLkDealsPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.onLkDealsPage().getDeal(2).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена продавцом\nПерейти к объявлению"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку в попапе по кнопке «Отменить запрос»")
    public void shouldClickCancelRequestButtonInCancelPopup() {
        mockRule.with("desktop/SafeDealCancelRequestSellerAnotherBuyer").update();

        basePageSteps.onLkDealsPage().getDeal(2).button(CANCEL_REQUEST).click();
        shouldChangeCancelReason("Договорился с другим покупателем");

        basePageSteps.onLkDealsPage().getDeal(2).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтменена продавцом\nПерейти к объявлению"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Перейти к объявлению» для отклоненной покупателем сделки")
    public void shouldClickGoToOfferButtonWhenDealDeclinedBuyer() {
        basePageSteps.onLkDealsPage().getDeal(3).button(GO_TO_OFFER).click();

        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("mitsubishi").path("lancer").path("1113993894-cc0c10b8")
                .path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подтверждаем сделку по кнопке «Подтвердить»")
    public void shouldApproveDeal() {
        mockRule.with("desktop/SafeDealDealUpdateApproveSeller").update();

        basePageSteps.onLkDealsPage().getDeal(4).button("Подтвердить").click();

        urlSteps.testing().path(DEAL).path(DEAL_ID_4).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку по кнопке «Отклонить» с причиной «Договорился с другим продавцом»")
    public void shouldDeclineDealWithAnotherBuyerReason() {
        mockRule.with("desktop/SafeDealDealUpdateSellerDeclineAnotherBuyer").update();

        basePageSteps.onLkDealsPage().getDeal(4).button("Отклонить").click();
        shouldChangeCancelReason("Договорился с другим покупателем");

        basePageSteps.onLkDealsPage().getDeal(4).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена продавцом\nПерейти к объявлению"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отменяем сделку по кнопке «Отклонить» с причиной «Другое»")
    public void shouldDeclineDealWithAnotherReason() {
        mockRule.with("desktop/SafeDealDealUpdateSellerDeclineAnotherReason").update();

        basePageSteps.onLkDealsPage().getDeal(4).button("Отклонить").click();
        basePageSteps.onLkDealsPage().popup().selectItem("Выберите причину", "Другое");
        basePageSteps.onLkDealsPage().popup().input("Опишите причину", "test test");
        basePageSteps.onLkDealsPage().popup().button("Отправить").click();

        basePageSteps.onLkDealsPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
        basePageSteps.onLkDealsPage().getDeal(4).should(hasText(
                "Mitsubishi Lancer X Рестайлинг\nПродажа\nИмя покупателя\nЧастное лицо\nСумма\n450 000 ₽\n" +
                        "Статус сделки\nОтклонена продавцом\nПерейти к объявлению"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Перейти к объявлению» для отмененной продавцом сделки")
    public void shouldClickGoToOfferButtonWhenDealCanceledSeller() {
        basePageSteps.onLkDealsPage().getDeal(5).button(GO_TO_OFFER).click();

        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("porsche").path("911").path("1113961017-5f5d3095")
                .path(SLASH).shouldNotSeeDiff();
    }

    @Step("Выбираем причину отмены")
    public void shouldChangeCancelReason(String reason) {
        basePageSteps.onLkDealsPage().popup().selectItem("Выберите причину", reason);
        basePageSteps.onLkDealsPage().popup().button("Отправить").click();
        basePageSteps.onLkDealsPage().notifier().should(hasText("Спасибо! Вы помогаете стать Авто.ру ещё лучше"));
    }
}