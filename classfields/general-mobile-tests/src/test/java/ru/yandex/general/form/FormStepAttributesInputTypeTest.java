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
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Attribute.attribute;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.EmptyValue.emptyValue;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.NumberValue.numberValue;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Характеристики», атрибут типа «Инпут»")
@DisplayName("Тесты на экране «Характеристики», атрибут типа «Инпут»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepAttributesInputTypeTest {

    private static final String WORK_TIME = "Время работы от 1 до 60 час";
    private static final String HINT_LESS_THAN_ALLOWED = "Введите число не менее 1";
    private static final String HINT_MORE_THAN_ALLOWED = "Введите число не более 60";
    private static final String LESS_THAN_ALLOWED = "0";
    private static final String MORE_THAN_ALLOWED = "61";
    private static final String MINIMAL_ALLOWED = "1";
    private static final String MAXIMAL_ALLOWED = "60";
    private static final String WORK_TIME_ID = "vremya-raboti_0_7nK1u9";
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
    @DisplayName("Инпут по дефолту - пустой")
    public void shouldSeeInputAttributeDefaultEmpty() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().should(hasValue(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число меньше разрешенного диапазона - ошибка")
    public void shouldSeeInputAttributeLessThanAllowed() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(LESS_THAN_ALLOWED);

        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint(HINT_LESS_THAN_ALLOWED)
                .should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число больше разрешенного диапазона - ошибка")
    public void shouldSeeInputAttributeMoreThanAllowed() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MORE_THAN_ALLOWED);

        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint(HINT_MORE_THAN_ALLOWED)
                .should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число на нижней границе разрешенного диапазона - нет ошибки")
    public void shouldSeeInputAttributeMinimumAllowed() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MINIMAL_ALLOWED);

        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число на верхней границе разрешенного диапазона - нет ошибки")
    public void shouldSeeInputAttributeMaximumAllowed() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MAXIMAL_ALLOWED);

        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число меньше разрешенного диапазона, жмем «Далее» - не переходим на следующий экран")
    public void shouldSeeInputAttributeLessThanAllowedCantNext() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(LESS_THAN_ALLOWED);
        offerAddSteps.onFormPage().button(NEXT).click();

        offerAddSteps.onFormPage().screenTitle().should(hasText(HARAKTERISTIKI));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число больше разрешенного диапазона, жмем «Далее» - не переходим на следующий экран")
    public void shouldSeeInputAttributeMoreThanAllowedCantNext() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MORE_THAN_ALLOWED);
        offerAddSteps.onFormPage().button(NEXT).click();

        offerAddSteps.onFormPage().screenTitle().should(hasText(HARAKTERISTIKI));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число меньше разрешенного диапазона, очищаем инпут - ошибка пропадает")
    public void shouldSeeClearInputAttributeLessThanAllowed() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(LESS_THAN_ALLOWED);
        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint().waitUntil(isDisplayed());
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().clearInput().click();

        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число больше разрешенного диапазона, очищаем инпут - ошибка пропадает")
    public void shouldSeeClearInputAttributeMoreThanAllowed() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MORE_THAN_ALLOWED);
        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint().waitUntil(isDisplayed());
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().clearInput().click();

        offerAddSteps.onFormPage().attribute(WORK_TIME).inputHint().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим значение типа «Инпут», без нажатия на «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterInputAttribute() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MAXIMAL_ALLOWED);

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(WORK_TIME_ID).setNumberValue(numberValue(Integer.valueOf(MAXIMAL_ALLOWED)))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим значение типа «Инпут», жмём на «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterInputAttributeAndNext() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MAXIMAL_ALLOWED);
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(7).setAttributes(asList(
                attribute().setId(WORK_TIME_ID).setNumberValue(numberValue(Integer.valueOf(MAXIMAL_ALLOWED)))));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем значение типа «Инпут», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearInputAttribute() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MAXIMAL_ALLOWED);
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().clearInput().click();

        expectedUpdateDraft.getForm().setAttributes(asList(
                attribute().setId(WORK_TIME_ID).setEmptyValue(emptyValue())));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим число - появляется кнопка «Дальше»")
    public void shouldSeeNextButtonAfterSetInputAttributeValue() {
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().sendKeys(MINIMAL_ALLOWED);

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).button(NEXT).should(isDisplayed());
    }

}
