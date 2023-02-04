package ru.yandex.general.form;

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
import ru.yandex.general.beans.ajaxRequests.updateDraft.Price;
import ru.yandex.general.beans.ajaxRequests.updateDraft.UpdateDraft;
import ru.yandex.general.consts.FormConstants;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Contacts.contacts;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.ADRES;
import static ru.yandex.general.mobile.page.FormPage.CALLS_AND_MESSAGES;
import static ru.yandex.general.mobile.page.FormPage.CENA_R;
import static ru.yandex.general.mobile.page.FormPage.CONTACTS;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.ONLY_CALLS;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getAddressTemplate;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Контакты»")
@DisplayName("Тесты на экране «Контакты»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepContactsTest {

    private static final String PHONE_CALL = "PhoneCall";
    private static final String ANY = "Any";
    private static final String CHAT = "Chat";
    private static final String[] JSONPATHS_TO_IGNORE = {"draftId", "form.photos[0].groupId", "form.photos[0].name",
            "form.photos[0].url", "form.photos[0].ratio", "form.addresses"};

    private UpdateDraft expectedUpdateDraft;
    private FormConstants.Categories category;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        category = FormConstants.Categories.UMNIE_KOLONKI;
        expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(12)
                .setCategoryPreset(GOODS).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)))
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setCondition(USED_CONDITION)
                .setPrice(Price.price().setFree(false))
                .setAddresses(asList(getAddressTemplate()));
        offerAddSteps.setMoscowCookie();
        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).fillToContactsStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оставляем «Только сообщения», жмём «Дальше», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterWithDefaultChatAfterNext() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CONTACTS).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(13).setContacts(
                contacts().setPreferredWayToContact(CHAT).setPhone("")
                        .setEmail(passportSteps.getCurrentAccountMail()));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем «Только звонки», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChangeToOnlyCalls() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().contactType(ONLY_CALLS).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setContacts(
                contacts().setPreferredWayToContact(PHONE_CALL).setPhone("")
                        .setEmail(passportSteps.getCurrentAccountMail()));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем «Звонки и сообщения», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChangeToCallsAndMessages() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().contactType(CALLS_AND_MESSAGES).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setContacts(
                contacts().setPreferredWayToContact(ANY).setPhone("")
                        .setEmail(passportSteps.getCurrentAccountMail()));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Выйти» на выборе контактов")
    public void shouldSeeLogout() {
        offerAddSteps.onFormPage().screen(CONTACTS).spanLink("Выйти").click();

        offerAddSteps.onFormPage().h1().should(hasText(FORM_PAGE_H1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Цена» по кнопке «Назад»")
    public void shouldSeeBackToPriceScreen() {
        offerAddSteps.onFormPage().screen(CONTACTS).back().click();

        offerAddSteps.onFormPage().screen(CENA_R).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем контакты, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().contactType(ONLY_CALLS).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().screen(CONTACTS).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(13).setContacts(
                contacts().setPreferredWayToContact("PhoneCall").setPhone("")
                        .setEmail(passportSteps.getCurrentAccountMail()));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем контакты, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().contactType(ONLY_CALLS).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().screen(CONTACTS).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем контакты, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().contactType(ONLY_CALLS).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().screen(CONTACTS).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем контакты, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().contactType(ONLY_CALLS).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().screen(CONTACTS).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, не меняя контакты, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(CONTACTS).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открывается экран «Адрес» после экрана контактов")
    public void shouldSeeAddressScreenAfterContacts() {
        offerAddSteps.onFormPage().screen(CONTACTS).button(NEXT).click();

        offerAddSteps.onFormPage().screen(ADRES).should(isDisplayed());
    }

}
