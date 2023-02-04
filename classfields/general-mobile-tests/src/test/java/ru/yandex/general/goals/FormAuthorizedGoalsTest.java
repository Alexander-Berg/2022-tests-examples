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
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_ADDRESS_MORE_CLICK;
import static ru.yandex.general.consts.Goals.FORM_OFFER_ADDRESS_READY;
import static ru.yandex.general.consts.Goals.FORM_OFFER_ADDRESS_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_CHAT;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_PHONE;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_PHONE_OR_CHAT;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTACTS_SHOW;
import static ru.yandex.general.consts.Goals.FORM_OFFER_CONTENT_READY_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ADD;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.ADD_MORE_ADDRESS;
import static ru.yandex.general.mobile.page.FormPage.CALLS_AND_MESSAGES;
import static ru.yandex.general.mobile.page.FormPage.ONLY_CALLS;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на форме")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormAuthorizedGoalsTest {

    private static final String ADDRESS = "проспект Карла Маркса, 2";
    private static final String SECOND_ADDRESS = "ул. Ленина, 12";

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
        passportSteps.createAccountAndLogin();
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTACTS_SHOW)
    @DisplayName("Цель «FORM_OFFER_CONTACTS_SHOW»")
    public void shouldSeeFormOfferPriceReadyGoal() {
        offerAddSteps.fillToContactsStep();

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
        offerAddSteps.fillToContactsStep();
        offerAddSteps.stepButtonClick();

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
        offerAddSteps.fillToContactsStep();
        offerAddSteps.onFormPage().contactType(ONLY_CALLS).waitUntil(isDisplayed()).click();

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
        offerAddSteps.fillToContactsStep();
        offerAddSteps.onFormPage().contactType(CALLS_AND_MESSAGES).waitUntil(isDisplayed()).click();

        goalsSteps.withGoalType(FORM_OFFER_CONTACTS_PHONE_OR_CHAT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_ADDRESS_SHOW)
    @DisplayName("Цель «FORM_OFFER_ADDRESS_SHOW»")
    public void shouldSeeFormOfferAddressShowGoal() {
        offerAddSteps.fillToAddressStep();

        goalsSteps.withGoalType(FORM_OFFER_ADDRESS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_ADDRESS_MORE_CLICK)
    @DisplayName("Цель «FORM_OFFER_ADDRESS_MORE_CLICK»")
    public void shouldSeeFormOfferAddressMoreClickGoal() {
        offerAddSteps.fillToAddressStep();
        offerAddSteps.addAddress(ADDRESS);
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).click();

        goalsSteps.withGoalType(FORM_OFFER_ADDRESS_MORE_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_ADDRESS_READY)
    @DisplayName("Цель «FORM_OFFER_ADDRESS_READY»")
    public void shouldSeeFormOfferAddressReadyGoal() {
        offerAddSteps.withCategory(PERENOSKA);
        offerAddSteps.fillToAddressStep();
        offerAddSteps.fillAddress();

        goalsSteps.withGoalType(FORM_OFFER_ADDRESS_READY)
                .withPageRef(urlSteps.testing().path(ADD).path(PERENOSKA.getCategoryPath()).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_ADDRESS_READY)
    @DisplayName("Цель «FORM_OFFER_ADDRESS_READY» категория работы")
    public void shouldSeeFormOfferAddressReadyGoalWorkCategory() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle());
        offerAddSteps.fillToAddressStep();
        offerAddSteps.fillAddress();

        goalsSteps.withGoalType(FORM_OFFER_ADDRESS_READY)
                .withPageRef(urlSteps.testing().path(ADD).path(REZUME_IN_SELLING.getCategoryPath()).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_ADDRESS_READY)
    @DisplayName("Цель «FORM_OFFER_ADDRESS_READY» с двумя адресами")
    public void shouldSeeFormOfferAddressReadyGoal2Addresses() {
        offerAddSteps.withCategory(PERENOSKA).withAddress(ADDRESS, SECOND_ADDRESS);
        offerAddSteps.fillToAddressStep();
        offerAddSteps.fillAddress();

        goalsSteps.withGoalType(FORM_OFFER_ADDRESS_READY)
                .withPageRef(urlSteps.testing().path(ADD).path(PERENOSKA.getCategoryPath()).toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTENT_READY_SHOW)
    @DisplayName("Цель «FORM_OFFER_CONTENT_READY_SHOW»")
    public void shouldSeeFormOfferContentReadyGoal() {
        offerAddSteps.fillToFinalStep();

        goalsSteps.withGoalType(FORM_OFFER_CONTENT_READY_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FORM_OFFER_CONTENT_READY_SHOW)
    @DisplayName("Цель «FORM_OFFER_CONTENT_READY_SHOW» категория работы")
    public void shouldSeeFormOfferContentReadyGoalWorkCategory() {
        offerAddSteps.withCategory(REZUME_IN_SELLING);
        offerAddSteps.fillToAddressStep();
        offerAddSteps.fillAddress();

        goalsSteps.withGoalType(FORM_OFFER_CONTENT_READY_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
