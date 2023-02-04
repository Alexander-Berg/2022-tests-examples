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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.page.OfferCardPage.COMPLAIN;
import static ru.yandex.general.page.OfferCardPage.NEXT;
import static ru.yandex.general.page.OfferCardPage.SEND;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Жалоба")
@DisplayName("Тесты на «Пожаловаться»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardComplainTest {

    private static final String THANKS = "Спасибо за бдительность!";
    private static final String GOOD = "Хорошо";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().spanLink(COMPLAIN).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Далее» не отображается без выбора причины")
    public void shouldSeeNextButtonDisabledWithoutChoice() {
        basePageSteps.onOfferCardPage().popup().button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Далее» появляется после выбора причины")
    public void shouldSeeNextButtonActiveAfterChoice() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Товар уже продан").waitUntil(isDisplayed()).click();

        basePageSteps.onOfferCardPage().popup().button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается textarea с плейсхолдером после выбора причины и перехода дальше")
    public void shouldSeeTextareaWithPlaceholderAfterNext() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Товар уже продан").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();

        basePageSteps.onOfferCardPage().popup().textarea().should(hasAttribute("placeholder", "Хотите что-то добавить?"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» отображается для конкретной причины жалобы без текста")
    public void shouldSeeEmptyMessageSendButtonActive() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Товар уже продан").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();

        basePageSteps.onOfferCardPage().popup().button(SEND).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» не отображается для «Другая причина» без уточнения")
    public void shouldSeeEmptyMessageSendButtonDisabledForAnotherReason() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();

        basePageSteps.onOfferCardPage().popup().button(SEND).should(hasAttribute("aria-disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» появляется для «Другая причина» при вводе уточнения")
    public void shouldSeeSendButtonActiveForAnotherReasonWithMessage() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();
        basePageSteps.onOfferCardPage().popup().textarea().sendKeys("Причина");

        basePageSteps.onOfferCardPage().popup().button(SEND).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется жалоба по причине «Цена указана неверно» без ввода сообщения")
    public void shouldSeeSendedComplainWithoutMessage() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();
        basePageSteps.onOfferCardPage().popup().button(SEND).click();

        basePageSteps.onOfferCardPage().popup().spanLink(THANKS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется жалоба по причине «Цена указана неверно» с сообщением")
    public void shouldSeeSendedComplainWithMessage() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();
        basePageSteps.onOfferCardPage().popup().textarea().sendKeys("Причина");
        basePageSteps.onOfferCardPage().popup().button(SEND).click();

        basePageSteps.onOfferCardPage().popup().spanLink(THANKS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется жалоба по причине «Другая причина» с сообщением")
    public void shouldSeeSendedComplainWithAnotherReasonAndMessage() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();
        basePageSteps.onOfferCardPage().popup().textarea().sendKeys("Причина");
        basePageSteps.onOfferCardPage().popup().button(SEND).click();

        basePageSteps.onOfferCardPage().popup().spanLink(THANKS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрывается модалка жалобы после отправки, по тапу на «Хорошо»")
    public void shouldSeeClosedModal() {
        basePageSteps.onOfferCardPage().popup().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().popup().button(NEXT).click();
        basePageSteps.onOfferCardPage().popup().button(SEND).click();
        basePageSteps.onOfferCardPage().popup().button(GOOD).click();

        basePageSteps.onOfferCardPage().popup().should(not(isDisplayed()));
    }

}
