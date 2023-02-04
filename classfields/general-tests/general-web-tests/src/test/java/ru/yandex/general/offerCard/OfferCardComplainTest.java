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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
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
@GuiceModules(GeneralWebModule.class)
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
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().button(COMPLAIN).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Далее» не активна без выбора причины")
    public void shouldSeeNextButtonDisabledWithoutChoice() {
        basePageSteps.onOfferCardPage().modal().button(NEXT).should(hasAttribute("aria-disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Далее» активируется после выбора причины")
    public void shouldSeeNextButtonActiveAfterChoice() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Товар уже продан").waitUntil(isDisplayed()).click();

        basePageSteps.onOfferCardPage().modal().button(NEXT).should(hasAttribute("aria-disabled", "false"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается textarea с плейсхолдером после выбора причины и перехода дальше")
    public void shouldSeeTextareaWithPlaceholderAfterNext() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Товар уже продан").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();

        basePageSteps.onOfferCardPage().modal().textarea().should(hasAttribute("placeholder", "Хотите что-то добавить?"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» активна для конкретной причины жалобы без текста")
    public void shouldSeeEmptyMessageSendButtonActive() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Товар уже продан").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();

        basePageSteps.onOfferCardPage().modal().button(SEND).should(hasAttribute("aria-disabled", "false"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» не активна для «Другая причина» без уточнения")
    public void shouldSeeEmptyMessageSendButtonDisabledForAnotherReason() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();

        basePageSteps.onOfferCardPage().modal().button(SEND).should(hasAttribute("aria-disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Отправить» активируется для «Другая причина» при вводе уточнения")
    public void shouldSeeSendButtonActiveForAnotherReasonWithMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().textarea().sendKeys("Причина");

        basePageSteps.onOfferCardPage().modal().button(SEND).should(hasAttribute("aria-disabled", "false"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется жалоба по причине «Цена указана неверно» без ввода сообщения")
    public void shouldSeeSendedComplainWithoutMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        basePageSteps.onOfferCardPage().modal().spanLink(THANKS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется жалоба по причине «Цена указана неверно» с сообщением")
    public void shouldSeeSendedComplainWithMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().textarea().sendKeys("Причина");
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        basePageSteps.onOfferCardPage().modal().spanLink(THANKS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется жалоба по причине «Другая причина» с сообщением")
    public void shouldSeeSendedComplainWithAnotherReasonAndMessage() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Другая причина").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().textarea().sendKeys("Причина");
        basePageSteps.onOfferCardPage().modal().button(SEND).click();

        basePageSteps.onOfferCardPage().modal().spanLink(THANKS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрывается модалка жалобы после отправки, по тапу на «Хорошо»")
    public void shouldSeeClosedModal() {
        basePageSteps.onOfferCardPage().modal().radioButtonWithLabel("Цена указана неверно").waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().modal().button(NEXT).click();
        basePageSteps.onOfferCardPage().modal().button(SEND).click();
        basePageSteps.onOfferCardPage().modal().button(GOOD).click();

        basePageSteps.onOfferCardPage().modal().should(not(isDisplayed()));
    }

}
