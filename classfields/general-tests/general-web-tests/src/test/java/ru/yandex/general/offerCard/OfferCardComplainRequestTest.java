package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.ajaxRequests.CreateComplaint;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.beans.ajaxRequests.CreateComplaint.createComplaint;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.page.OfferCardPage.COMPLAIN;
import static ru.yandex.general.page.OfferCardPage.NEXT;
import static ru.yandex.general.page.OfferCardPage.SEND;
import static ru.yandex.general.step.AjaxProxySteps.CREATE_COMPLAINT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Жалоба")
@DisplayName("Тесты на «Пожаловаться». Проверка запроса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class OfferCardComplainRequestTest {

    private static final String TEXT = "Текст причины";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().button(COMPLAIN).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса. Отправляется жалоба по причине «Цена указана неверно» без ввода сообщения")
    public void shouldSeeSendedComplainWithoutMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        ajaxProxySteps.setAjaxHandler(CREATE_COMPLAINT).withRequestText(
                getComplaint().setReason("PriceError").setText("")).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса. Отправляется жалоба по причине «Цена указана неверно» с сообщением")
    public void shouldSeeSendedComplainWithMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().textarea().sendKeys(TEXT);
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        ajaxProxySteps.setAjaxHandler(CREATE_COMPLAINT).withRequestText(
                getComplaint().setReason("PriceError").setText(TEXT)).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса. Отправляется жалоба по причине «Другая причина» с сообщением")
    public void shouldSeeSendedComplainWithAnotherReasonAndMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().textarea().sendKeys(TEXT);
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        ajaxProxySteps.setAjaxHandler(CREATE_COMPLAINT).withRequestText(
                getComplaint().setReason("Another").setText(TEXT)).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса. Отправляется жалоба по причине «Мошенник» без ввода сообщения")
    public void shouldSeeSendedComplainFraudWithoutMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Мошенник").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        ajaxProxySteps.setAjaxHandler(CREATE_COMPLAINT).withRequestText(
                getComplaint().setReason("UserFraud").setText("")).shouldExist();
    }

    private CreateComplaint getComplaint() {
        return createComplaint().setPlacement("offerCard").setApplication("Web")
                .setOfferId(urlSteps.getOfferId());
    }

}
