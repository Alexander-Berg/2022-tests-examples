package ru.yandex.general.goals;

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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_CHAT;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_PHONE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_PHONE_OR_CHAT;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.PUBLISH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на блок контактов на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FormContactsGoalsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTACTS_SHOW)
    @DisplayName("Цель «FORM_OFFER_CONTACTS_SHOW»")
    public void shouldSeeFormOfferPriceReadyGoal() {
        offerAddSteps.fillToAttributesStep();

        goalsSteps.withGoalType(FORM_OFFER_CONTACTS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTACTS_CHAT)
    @DisplayName("Цель «FORM_OFFER_CONTACTS_CHAT»")
    public void shouldSeeFormOfferContactsChatGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.nextClick().fillAddress();
        offerAddSteps.scrollingToElement(offerAddSteps.onFormPage().button(PUBLISH));

        goalsSteps.withGoalType(FORM_OFFER_CONTACTS_CHAT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTACTS_PHONE)
    @DisplayName("Цель «FORM_OFFER_CONTACTS_PHONE»")
    public void shouldSeeFormOfferContactsPhoneGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.onFormPage().contactType("Только звонки").waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_CONTACTS_PHONE)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTACTS_PHONE_OR_CHAT)
    @DisplayName("Цель «FORM_OFFER_CONTACTS_PHONE_OR_CHAT»")
    public void shouldSeeFormOfferContactsPhoneOrChatGoal() {
        offerAddSteps.fillToAttributesStep();
        offerAddSteps.onFormPage().contactType("Звонки и сообщения").waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_CONTACTS_PHONE_OR_CHAT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
