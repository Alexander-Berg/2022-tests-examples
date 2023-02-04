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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Attribute.attribute;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.EmptyValue.emptyValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.RepeatedDictionaryValue.repeatedDictionaryValue;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.element.Popup.SELECTED;
import static ru.yandex.general.mobile.page.FormPage.HARAKTERISTIKI;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Характеристики», атрибут типа «Мультиселект»")
@DisplayName("Тесты на экране «Характеристики», атрибут типа «Мультиселект»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepAttributesMultiselectTypeTest {

    private static final String TIP_SISTEMY = "Тип акустической системы";
    private static final String SUBWOOFER = "сабвуфер";
    private static final String SUBWOOFER_ID = "sabvufer";
    private static final String TWITER = "твитер";
    private static final String TWITER_ID = "tviter";
    private static final String TIP_SISTEMY_ID = "tip-akusticheskoy-sistemi_4897742_gc6UKP";
    private static final String FILTRATION_SEGMENT = "са";
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
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
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
    @DisplayName("Открываем попап выбора значения атрибута типа «Мультиселект»")
    public void shouldSeeMultiselectAttributePopup() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup(TIP_SISTEMY).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап выбора значения атрибута типа «Мультиселект»")
    public void shouldCloseMultiselectAttributePopup() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтруем айтемы в попапе выбора значения атрибута типа «Мультиселект»")
    public void shouldFilerItemsMultiselectAttributePopup() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        int itemsCountBeforeFiltration = offerAddSteps.onFormPage().popup(TIP_SISTEMY).getItemsCount();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().sendKeys(FILTRATION_SEGMENT);
        offerAddSteps.wait500MS();
        int itemsCountAfterFiltration = offerAddSteps.onFormPage().popup(TIP_SISTEMY).getItemsCount();

        assertThat(itemsCountAfterFiltration).isLessThan(itemsCountBeforeFiltration);
        assertThat(offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItems()).allMatch(
                item -> item.getText().contains(FILTRATION_SEGMENT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отменяем фильтр айтемов в попапе выбора значения атрибута типа «Мультиселект»")
    public void shouldCancelFilerItemsMultiselectAttributePopup() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        int itemsCountBeforeFiltration = offerAddSteps.onFormPage().popup(TIP_SISTEMY).getItemsCount();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().sendKeys(FILTRATION_SEGMENT);
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().clearInput().click();
        offerAddSteps.wait500MS();
        int itemsCountAfterCancelFiltration = offerAddSteps.onFormPage().popup(TIP_SISTEMY).getItemsCount();

        assertThat(itemsCountAfterCancelFiltration).isEqualTo(itemsCountBeforeFiltration);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбранное значение «Мультиселекта» отображается в инпуте попапа")
    public void shouldSeeChosedMultiselectValueInInput() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().should(hasValue(format("%s, ", SUBWOOFER)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбранное значение «Мультиселекта» после переоткрытия попапа остается выбранным")
    public void shouldSeeChosedMultiselectValueInInputAfterReopenPopup() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).should(hasClass(containsString(SELECTED)));
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().should(hasValue(format("%s, ", SUBWOOFER)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Два выбранных значения «Мультиселекта» отображаются в инпуте попапа")
    public void shouldSeeTwoChosedMultiselectValueInInput() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(TWITER).click();

        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().should(
                hasValue(format("%s, %s, ", SUBWOOFER, TWITER)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Мультиселект», не закрываем попап, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseMultiselectAttribute() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setRepeatedDictionaryValue(repeatedDictionaryValue(SUBWOOFER_ID))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем два значения типа «Мультиселект», не закрываем попап, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseTwoMultiselectAttribute() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(TWITER).click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setRepeatedDictionaryValue(
                        repeatedDictionaryValue(SUBWOOFER_ID, TWITER_ID))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем два значения типа «Мультиселект», закрываем попап, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseTwoMultiselectAttributeAndClosePopup() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(TWITER).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setRepeatedDictionaryValue(
                        repeatedDictionaryValue(SUBWOOFER_ID, TWITER_ID))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Мультиселект», жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseMultiselectAttributeAndNext() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setRepeatedDictionaryValue(repeatedDictionaryValue(SUBWOOFER_ID))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем два значения типа «Мультиселект», жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseTwoMultiselectAttributeAndNext() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(TWITER).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setRepeatedDictionaryValue(
                        repeatedDictionaryValue(SUBWOOFER_ID, TWITER_ID))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Мультиселект», очищаем в инпуте, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearMultiselectAttributeInput() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).clearButton().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setEmptyValue(emptyValue())));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Мультиселект», очищаем в попапе, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearMultiselectAttributeInPopup() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setEmptyValue(emptyValue())));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение, фильтруем айтемы в попапе, выбираем ещё значение, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterFilerItemsMultiselectAttribute() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(TWITER).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).input().sendKeys(FILTRATION_SEGMENT);
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();

        expectedUpdateDraft.getForm().setCurrentControlNum(6).setAttributes(asList(
                attribute().setId(TIP_SISTEMY_ID).setRepeatedDictionaryValue(
                        repeatedDictionaryValue(SUBWOOFER_ID, TWITER_ID))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Мультиселект» - появляется кнопка «Дальше»")
    public void shouldSeeNextButtonAfterSetInputAttributeValue() {
        offerAddSteps.onFormPage().attribute(TIP_SISTEMY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).menuItem(SUBWOOFER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_SISTEMY).closeFloatPopup().click();

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).should(isDisplayed());
    }

}
