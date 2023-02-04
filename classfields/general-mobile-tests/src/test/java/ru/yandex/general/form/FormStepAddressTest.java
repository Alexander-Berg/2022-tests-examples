package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.ArrayUtils;
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
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Address.address;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Contacts.contacts;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.GeoPoint.geoPoint;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_NOVOSIBIRSK_H1;
import static ru.yandex.general.mobile.page.FormPage.ADD_MORE_ADDRESS;
import static ru.yandex.general.mobile.page.FormPage.ADRES;
import static ru.yandex.general.mobile.page.FormPage.COMPLETE;
import static ru.yandex.general.mobile.page.FormPage.CONTACTS;
import static ru.yandex.general.mobile.page.FormPage.DELIVERY;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getAddressTemplate;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Адрес»")
@DisplayName("Тесты на экране «Адрес»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepAddressTest {

    private static final String ADDRESS = "ул. Ленина, 12";
    private static final String VTOROI_ADRES = "Второй адрес";

    private static final String[] JSONPATHS_TO_IGNORE = {"draftId", "form.photos[0].groupId", "form.photos[0].name",
            "form.photos[0].url", "form.photos[0].ratio"};
    private static final String[] GEO_IGNORE = {"form.addresses[0].district", "form.addresses[0].metroStation", "form.addresses[0].region"};

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
        passportSteps.accountForOfferCreationLogin();
        expectedUpdateDraft.getForm().setCurrentControlNum(13)
                .setCategoryPreset(GOODS).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)))
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setCondition(USED_CONDITION)
                .setPrice(Price.price().setFree(false))
                .setContacts(contacts().setPreferredWayToContact("Chat").setPhone("")
                        .setEmail(passportSteps.getCurrentAccountMail()));
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).fillToAddressStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем второй адрес, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterAddSecondAddress() {
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().wrapper(VTOROI_ADRES).input().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(ADDRESS).click();
        offerAddSteps.onFormPage().wrapper(VTOROI_ADRES).button(COMPLETE).click();


        expectedUpdateDraft.getForm().setAddresses(asList(
                getAddressTemplate(),
                address().setGeoPoint(geoPoint().setLatitude(55.029766).setLongitude(82.913422))
                        .setAddress(addressText().setAddress(ADDRESS))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(ArrayUtils.addAll(JSONPATHS_TO_IGNORE, GEO_IGNORE))
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем адрес, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChangeAddress() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().input().clearInput().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(ADDRESS).click();

        expectedUpdateDraft.getForm().setAddresses(asList(
                address().setGeoPoint(geoPoint().setLatitude(55.029766).setLongitude(82.913422))
                        .setAddress(addressText().setAddress(ADDRESS))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Контакты» по кнопке «Назад»")
    public void shouldSeeBackToContactsScreen() {
        offerAddSteps.onFormPage().screen(ADRES).back().click();

        offerAddSteps.onFormPage().screen(CONTACTS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем адрес, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().input().clearInput().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(ADDRESS).click();

        offerAddSteps.onFormPage().screen(ADRES).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(14).setAddresses(asList(
                address().setGeoPoint(geoPoint().setLatitude(55.029766).setLongitude(82.913422))
                        .setAddress(addressText().setAddress(ADDRESS))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем адрес, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().input().clearInput().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(ADDRESS).click();

        offerAddSteps.onFormPage().screen(ADRES).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем адрес, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().input().clearInput().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(ADDRESS).click();
        offerAddSteps.onFormPage().screen(ADRES).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_NOVOSIBIRSK_H1));
        urlSteps.testing().path(NOVOSIBIRSK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем адрес, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().input().clearInput().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(ADDRESS).click();
        offerAddSteps.onFormPage().screen(ADRES).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_NOVOSIBIRSK_H1));
        urlSteps.testing().path(NOVOSIBIRSK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, не меняя адрес, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(ADRES).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_NOVOSIBIRSK_H1));
        urlSteps.testing().path(NOVOSIBIRSK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открывается экран «Доставка» после экрана адреса. Товарная категория")
    public void shouldSeeDeliveryScreenAfterAddressGoodsCategory() {
        offerAddSteps.onFormPage().screen(ADRES).button(NEXT).click();

        offerAddSteps.onFormPage().screen(DELIVERY).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открываем добавление второго адреса, не вводим, жмем «Готово», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterOpenSecondAddressForm() {
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().wrapper(VTOROI_ADRES).button(COMPLETE).click();
        offerAddSteps.onFormPage().screen(ADRES).button(NEXT).click();


        expectedUpdateDraft.getForm().setAddresses(asList(
                getAddressTemplate())).setCurrentControlNum(14);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(ArrayUtils.addAll(JSONPATHS_TO_IGNORE, GEO_IGNORE))
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

}
