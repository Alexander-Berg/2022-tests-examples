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
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_NOVOSIBIRSK_H1;
import static ru.yandex.general.mobile.page.FormPage.ADRES;
import static ru.yandex.general.mobile.page.FormPage.DELIVERY;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.WE_READY_PUBLISH;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getAddressTemplate;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Мы готовы! Публикуем?»")
@DisplayName("Тесты на экране «Мы готовы! Публикуем?»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepFinalTest {

    private static final String[] JSONPATHS_TO_IGNORE = {"draftId", "form.photos[0].groupId", "form.photos[0].name",
            "form.photos[0].url", "form.photos[0].ratio", "form.addresses[0].district", "form.addresses[0].metroStation", "form.addresses[0].region"};

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
                        .setEmail(passportSteps.getCurrentAccountMail()))
                .setAddresses(asList(getAddressTemplate()));
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).fillToFinalStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Доставка» по кнопке «Назад»")
    public void shouldSeeBackToAddressScreen() {
        offerAddSteps.onFormPage().screen(WE_READY_PUBLISH).back().click();

        offerAddSteps.onFormPage().screen(DELIVERY).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(WE_READY_PUBLISH).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_NOVOSIBIRSK_H1));
        urlSteps.testing().path(NOVOSIBIRSK).shouldNotDiffWithWebDriverUrl();
    }

}
