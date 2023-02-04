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
import ru.yandex.general.beans.ajaxRequests.updateDraft.UpdateDraft;
import ru.yandex.general.consts.FormConstants;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Attribute.attribute;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.BooleanValue.booleanValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.HARAKTERISTIKI;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Характеристики», атрибут типа «Чекбокс»")
@DisplayName("Тесты на экране «Характеристики», атрибут типа «Чекбокс»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepAttributesCheckboxTypeTest {

    private static final String BLUETOOTH = "Bluetooth";
    private static final String BLUETOOTH_ID = "bluetooth_xz35Pm";
    private static final String[] JSONPATHS_TO_IGNORE = {"draftId", "form.photos[0].groupId", "form.photos[0].name", "form.photos[0].url", "form.photos[0].ratio"};

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

    @Before
    public void before() {
        category = FormConstants.Categories.UMNIE_KOLONKI;
        expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(6)
                .setCategoryPreset(GOODS).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)))
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setCondition(USED_CONDITION);
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).fillToAttributesStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим значение типа «Чекбокс», без нажатия на «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterCheckboxAttribute() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(BLUETOOTH_ID).setBooleanValue(booleanValue(true))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим значение типа «Чекбокс», жмём на «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterCheckboxAttributeAndNext() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(BLUETOOTH_ID).setBooleanValue(booleanValue(true))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим значение типа «Чекбокс», очищаем, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearCheckboxAttribute() {
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(BLUETOOTH_ID).setBooleanValue(booleanValue(false))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Активируем чекбокс - появляется кнопка «Дальше»")
    public void shouldSeeNextButtonAfterSetInputAttributeValue() {
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).should(isDisplayed());
    }

}
