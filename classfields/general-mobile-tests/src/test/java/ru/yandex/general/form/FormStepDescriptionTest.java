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
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.Input.PLACEHOLDER;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.CATEGORY;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.OPISANIE;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.mobile.page.FormPage.SKIP;
import static ru.yandex.general.mobile.page.FormPage.VIDEO;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.NULL_STRING;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Описание»")
@DisplayName("Тесты на экране «Описание»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepDescriptionTest {

    private static final String DESCRIPTION_PLACEHOLDER = "Мы не ограничиваем ваши творческие порывы! Но просим не " +
            "указывать ссылки и свои контактные данные: мошенники только этого и ждут";
    private static final String DESCRIPTION = "Пример описания 12345!? Test@æ«»";

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
        category = FormConstants.Categories.PERENOSKA;
        expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(3)
                .setCategoryPreset(GOODS).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)))
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle());
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).withName(category.getTitle()).fillToDescriptionStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Плейсхолдер инпута описания")
    public void shouldSeeDescriptionInputPlaceholder() {
        offerAddSteps.onFormPage().screen(OPISANIE).textarea().should(hasAttribute(PLACEHOLDER, DESCRIPTION_PLACEHOLDER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Пропустить» без описания")
    public void shouldSeeSkipButtonWithoutDescription() {
        offerAddSteps.onFormPage().screen(OPISANIE).button(SKIP).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Дальше» без описания")
    public void shouldNotSeeNextButtonWithoutDescription() {
        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Пропустить» с описанием")
    public void shouldNotSeeSkipButtonWithDescription() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);

        offerAddSteps.onFormPage().screen(OPISANIE).button(SKIP).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Дальше» с описанием")
    public void shouldSeeNextButtonWithDescription() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);

        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Пропустить» после очистки описания")
    public void shouldSeeSkipButtonAfterClearDescription() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).waitUntil(isDisplayed());
        offerAddSteps.onFormPage().clearTextarea().click();

        offerAddSteps.onFormPage().screen(OPISANIE).button(SKIP).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Дальше» после очистки описания")
    public void shouldNotSeeNextButtonAfterClearDescription() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).waitUntil(isDisplayed());
        offerAddSteps.onFormPage().clearTextarea().click();

        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем описание, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterAddDescription() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setDescription(DESCRIPTION);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем описание, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearDescription() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.waitSomething(3, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().clearTextarea().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setDescription("");
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем описание, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterAddDescriptionAndNext() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setDescription(DESCRIPTION).setCurrentControlNum(4);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем описание, жмем «Пропустить», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterSkipDescriptionAndSkipButtonClick() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(OPISANIE).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setDescription(NULL_STRING).setCurrentControlNum(4);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Видео», после «Пропустить» с экрана добавления описания")
    public void shouldSeeVideoScreenAfterSkip() {
        offerAddSteps.onFormPage().screen(OPISANIE).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(VIDEO).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Видео», после «Дальше» с экрана добавления описания")
    public void shouldSeeVideoScreenAfterNext() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(VIDEO).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Категория» по кнопке «Назад»")
    public void shouldSeeBackToCategoryScreen() {
        offerAddSteps.onFormPage().screen(OPISANIE).back().click();

        offerAddSteps.onFormPage().screen(CATEGORY).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем описание, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(4).setDescription(DESCRIPTION);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем описание, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем описание, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем описание, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().textarea().sendKeys(DESCRIPTION);
        offerAddSteps.onFormPage().screen(OPISANIE).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем описание, закрываем форму, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(OPISANIE).close().click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
