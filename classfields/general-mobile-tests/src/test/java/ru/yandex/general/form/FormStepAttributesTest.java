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
import ru.yandex.general.beans.ajaxRequests.updateDraft.EmptyValue;
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
import static ru.yandex.general.beans.ajaxRequests.updateDraft.DictionaryValue.dictionaryValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.NumberValue.numberValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.RepeatedDictionaryValue.repeatedDictionaryValue;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.CENA;
import static ru.yandex.general.mobile.page.FormPage.HARAKTERISTIKI;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.mobile.page.FormPage.SKIP;
import static ru.yandex.general.mobile.page.FormPage.SOSTOYANIE;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Характеристики»")
@DisplayName("Тесты на экране «Характеристики»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepAttributesTest {

    private static final String VOICE_ASSISTANT = "Голосовой помощник";
    private static final String VOICE_ASSISTANT_ID = "golosovoy-pomoschnik_15561008_T1P3i0";
    private static final String APPLE_SIRI = "Apple Siri";
    private static final String AMAZON_ALEXA = "Amazon Alexa";
    private static final String MANUFACTURER = "Производитель";
    private static final String MANUFACTURER_ID = "proizvoditel-umnyh-kolonok_Njqq1d";
    private static final String APPLE = "Apple";
    private static final String TIP_PITANIYA = "Тип питания";
    private static final String TIP_PITANIYA_ID = "tip-pitaniya_8_hCo8gj";
    private static final String OT_SETI = "от сети";
    private static final String OT_AKKUMULATORA = "от аккумулятора";
    private static final String BLUETOOTH = "Bluetooth";
    private static final String BLUETOOTH_ID = "bluetooth_xz35Pm";
    private static final String WORK_TIME = "Время работы от 1 до 60 час";
    private static final String WORK_TIME_ID = "vremya-raboti_0_7nK1u9";
    private static final String WORK_TIME_VALUE = "31";
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
    @DisplayName("Отображается кнопка «Пропустить» без атрибутов")
    public void shouldSeeSkipButtonWithoutAttributes() {
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(SKIP).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем все атрибуты, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftWithAllAttributes() {
        offerAddSteps.onFormPage().attribute(VOICE_ASSISTANT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(VOICE_ASSISTANT).menuItem(APPLE_SIRI).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).menuItem(OT_SETI).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).closeFloatPopup().click();

        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);

        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(VOICE_ASSISTANT_ID).setDictionaryValue(dictionaryValue("apple-siri")),
                attribute().setId(MANUFACTURER_ID).setDictionaryValue(dictionaryValue(APPLE.toLowerCase())),
                attribute().setId(TIP_PITANIYA_ID).setRepeatedDictionaryValue(repeatedDictionaryValue("ot-seti")),
                attribute().setId(WORK_TIME_ID).setNumberValue(numberValue(Integer.valueOf(WORK_TIME_VALUE))),
                attribute().setId(BLUETOOTH_ID).setBooleanValue(booleanValue(true))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем все атрибуты, затем очищаем все атрибуты, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftWithClearAllAttributes() {
        offerAddSteps.onFormPage().attribute(VOICE_ASSISTANT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(VOICE_ASSISTANT).menuItem(APPLE_SIRI).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).menuItem(OT_SETI).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).closeFloatPopup().click();

        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);

        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        offerAddSteps.onFormPage().attribute(VOICE_ASSISTANT).clearButton().click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).clearButton().click();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).clearButton().click();
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().clearInput().click();
        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(VOICE_ASSISTANT_ID).setEmptyValue(EmptyValue.emptyValue()),
                attribute().setId(MANUFACTURER_ID).setEmptyValue(EmptyValue.emptyValue()),
                attribute().setId(TIP_PITANIYA_ID).setEmptyValue(EmptyValue.emptyValue()),
                attribute().setId(WORK_TIME_ID).setEmptyValue(EmptyValue.emptyValue()),
                attribute().setId(BLUETOOTH_ID).setBooleanValue(booleanValue(false))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем все атрибуты, меняем или очищаем часть, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftWithAllAttributesChangedOrClear() {
        offerAddSteps.onFormPage().attribute(VOICE_ASSISTANT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(VOICE_ASSISTANT).menuItem(APPLE_SIRI).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(MANUFACTURER).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(MANUFACTURER).menuItem(APPLE).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).menuItem(OT_SETI).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).closeFloatPopup().click();

        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);

        offerAddSteps.onFormPage().attribute(BLUETOOTH).checkboxEmptyLabel().click();

        offerAddSteps.onFormPage().attribute(VOICE_ASSISTANT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(VOICE_ASSISTANT).menuItem(AMAZON_ALEXA).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).menuItem(OT_SETI).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).menuItem(OT_AKKUMULATORA).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup(TIP_PITANIYA).closeFloatPopup().click();

        offerAddSteps.onFormPage().attribute(MANUFACTURER).clearButton().click();

        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(VOICE_ASSISTANT_ID).setDictionaryValue(dictionaryValue("amazon-alexa")),
                attribute().setId(MANUFACTURER_ID).setEmptyValue(EmptyValue.emptyValue()),
                attribute().setId(TIP_PITANIYA_ID).setRepeatedDictionaryValue(repeatedDictionaryValue("ot-akkumulyatora")),
                attribute().setId(WORK_TIME_ID).setNumberValue(numberValue(Integer.valueOf(WORK_TIME_VALUE))),
                attribute().setId(BLUETOOTH_ID).setBooleanValue(booleanValue(true))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём кнопку «Пропустить» без атрибутов, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterSkipButtonWithoutAttributes() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(SKIP).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList());
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем атрибут, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(11).setAttributes(asList(
                attribute().setId(WORK_TIME_ID).setNumberValue(numberValue(Integer.valueOf(WORK_TIME_VALUE)))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем атрибут, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем атрибут, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем атрибут, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(WORK_TIME_VALUE);
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем атрибут, закрываем форму, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).close().click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Состояние» по кнопке «Назад»")
    public void shouldSeeBackToConditionScreen() {
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).back().click();

        offerAddSteps.onFormPage().screen(SOSTOYANIE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Цена», для товарной категории, по клику на «Пропустить»")
    public void shouldSeePriceScreenAfterAttributesScreen() {
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(SKIP).click();

        offerAddSteps.onFormPage().screen(CENA).should(isDisplayed());
    }

}
