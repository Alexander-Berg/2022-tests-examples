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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.element.CardNotice.SAVE;
import static ru.yandex.general.page.OfferCardPage.COMPLAIN;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Заметки")
@DisplayName("Тесты на заметку")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardNoticeSaveTest {

    private static final String NOTICE_TEXT = "Заметка к офферу";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сохранение заметки на карточке")
    public void shouldSaveNoticeCard() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().notice().textarea().click();
        basePageSteps.onOfferCardPage().notice().textarea().sendKeys(NOTICE_TEXT);
        basePageSteps.onOfferCardPage().notice().button(SAVE).click();
        basePageSteps.onOfferCardPage().popupNotification("Заметка добавлена").waitUntil(isDisplayed());
        basePageSteps.refresh();

        basePageSteps.onOfferCardPage().notice().textarea().should(hasText(NOTICE_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаление заметки на карточке")
    public void shouldDeleteNoticeCard() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().notice().textarea().click();
        basePageSteps.onOfferCardPage().notice().textarea().sendKeys(NOTICE_TEXT);
        basePageSteps.onOfferCardPage().notice().button(SAVE).click();
        basePageSteps.onOfferCardPage().popupNotification("Заметка добавлена").waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().notice().hover();
        basePageSteps.onOfferCardPage().notice().trash().click();
        basePageSteps.onOfferCardPage().popupNotification("Заметка удалена").waitUntil(isDisplayed());
        basePageSteps.refresh();

        basePageSteps.onOfferCardPage().notice().textarea().should(hasText(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очистка заметки на карточке")
    public void shouldClearNoticeCard() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();
        basePageSteps.scrollingToElement(basePageSteps.onOfferCardPage().spanLink(COMPLAIN));
        basePageSteps.onOfferCardPage().notice().textarea().click();
        basePageSteps.onOfferCardPage().notice().textarea().sendKeys(NOTICE_TEXT);
        basePageSteps.onOfferCardPage().notice().clearTextarea().click();

        basePageSteps.onOfferCardPage().notice().button(SAVE).should(not(isDisplayed()));
        basePageSteps.onOfferCardPage().notice().textarea().should(hasText(""));
    }

}
