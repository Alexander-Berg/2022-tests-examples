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
import static ru.yandex.general.beans.ajaxRequests.updateDraft.DictionaryValue.dictionaryValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.EmptyValue.emptyValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Характеристики», атрибут типа «Селект»")
@DisplayName("Тесты на экране «Характеристики», атрибут типа «Селект»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepAttributesSelectTypeTest {

    private static final String MANUFACTURER = "Производитель";
    private static final String MANUFACTURER_ID = "proizvoditel-umnyh-kolonok_Njqq1d";
    private static final String APPLE = "Apple";
    private static final String BOSE = "Bose";
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
    @DisplayName("Открываем попап выбора значения атрибута типа «Селект»")
    public void shouldSeeSelectAttributePopup() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup(MANUFACTURER).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем попап выбора значения атрибута типа «Селект»")
    public void shouldCloseSelectAttributePopup() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        offerAddSteps.onFormPage().popup(MANUFACTURER).closeFloatPopup().click();

        offerAddSteps.onFormPage().popup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтруем айтемы в попапе выбора значения атрибута типа «Селект»")
    public void shouldFilerItemsSelectAttributePopup() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        int itemsCountBeforeFiltration = offerAddSteps.onFormPage().popup(MANUFACTURER).getItemsCount();
        offerAddSteps.onFormPage().popup(MANUFACTURER).input().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).input().sendKeys("A");
        offerAddSteps.wait500MS();
        int itemsCountAfterFiltration = offerAddSteps.onFormPage().popup(MANUFACTURER).getItemsCount();

        assertThat(itemsCountAfterFiltration).isLessThan(itemsCountBeforeFiltration);
        assertThat(offerAddSteps.onFormPage().popup(MANUFACTURER).menuItems()).allMatch(
                item -> (item.getText().contains("a") | item.getText().contains("A")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап атрибута типа «Селект» закрывается при выбора значения")
    public void shouldSeeClosedSelectAttributePopup() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup(MANUFACTURER).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбранное значение типа «Селект» отображается в инпуте")
    public void shouldSeeChosedSelectValueInInput() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(MANUFACTURER).inputText().should(
                hasText(format("%s\n%s", MANUFACTURER, APPLE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбранное значение «Селект» после переоткрытия попапа остается выбранным")
    public void shouldSeeChosedSelectValueInInputAfterReopenPopup() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).should(hasClass(containsString(SELECTED)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Селект», без нажатия на «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseSelectAttribute() {
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(MANUFACTURER_ID).setDictionaryValue(dictionaryValue(APPLE.toLowerCase()))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Селект», жмем на «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChoseSelectAttributeAndNext() {
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(MANUFACTURER_ID).setDictionaryValue(dictionaryValue(APPLE.toLowerCase()))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем значение типа «Селект», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearSelectAttribute() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).inputText().waitUntil(hasText(containsString(APPLE)));
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).clearButton().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(MANUFACTURER_ID).setEmptyValue(emptyValue())));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем значение типа «Селект», в инпуте не отображается выбранный текст")
    public void shouldClearSelectAttribute() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).inputText().waitUntil(hasText(containsString(APPLE)));
        offerAddSteps.onFormPage().attribute(MANUFACTURER).clearButton().click();

        offerAddSteps.onFormPage().attribute(MANUFACTURER).inputText().should(hasText(MANUFACTURER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Селект», затем меняем на другое, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChangeSelectAttribute() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).inputText().waitUntil(hasText(containsString(APPLE))).click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(BOSE).click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(MANUFACTURER_ID).setDictionaryValue(dictionaryValue(BOSE.toLowerCase()))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем значение типа «Селект» - появляется кнопка «Дальше»")
    public void shouldSeeNextButtonAfterSetInputAttributeValue() {
        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).should(isDisplayed());
    }

}
