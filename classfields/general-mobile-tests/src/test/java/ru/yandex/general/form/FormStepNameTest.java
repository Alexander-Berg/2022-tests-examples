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
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
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
import static ru.yandex.general.mobile.page.FormPage.ENTER_NAME;
import static ru.yandex.general.mobile.page.FormPage.NAZVANIE;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.PHOTOS;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Название»")
@DisplayName("Тесты на экране «Название»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepNameTest {

    private static final String NAME_STEP_TEXT = "Не сокращайте название и пишите его точно. Иначе ваше объявление никто не найдёт.";
    private static final String OFFER_NAME = "Название";
    private static final String EXTRA_NAME = " редактируемая часть";
    private static final String DEFAULT_NAME = "Предсказание не работает в тестовой среде";
    private static final String[] JSONPATHS_TO_IGNORE = {"draftId", "form.photos[0].groupId", "form.photos[0].name", "form.photos[0].url", "form.photos[0].ratio"};

    private UpdateDraft expectedUpdateDraft;

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
        expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(1).setCategoryPreset(GOODS).setPhotos(asList(
                photo().setNamespace(PHOTO_NAMESPACE)));
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.fillToNameStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается подсказка на экране добавления названия")
    public void shouldSeeHintNameStep() {
        offerAddSteps.onFormPage().text().should(hasText(NAME_STEP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Плейсхолдер инпута названия")
    public void shouldSeeNameInputPlaceholder() {
        offerAddSteps.onFormPage().screen(NAZVANIE).input().clearInput().click();

        offerAddSteps.onFormPage().screen(NAZVANIE).input().should(hasAttribute(PLACEHOLDER, "Введите название"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Дальше» без введенного названия")
    public void shouldNotSeeNextButtonWithoutPhoto() {
        offerAddSteps.onFormPage().screen(NAZVANIE).input().clearInput().click();

        offerAddSteps.onFormPage().screen(NAZVANIE).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Дальше» с названием")
    public void shouldSeeNextButtonWithName() {
        offerAddSteps.clearAndSetName(OFFER_NAME);

        offerAddSteps.onFormPage().screen(NAZVANIE).button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем название, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterAddName() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(OFFER_NAME);
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setTitle(OFFER_NAME);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем название, затем ждем и дополняем его, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterAddNameAndEditName() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(OFFER_NAME);
        offerAddSteps.waitSomething(3, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(EXTRA_NAME);
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setTitle(format("%s%s", NAZVANIE, EXTRA_NAME));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем название, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterAddNameAndNext() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(OFFER_NAME);
        offerAddSteps.onFormPage().screen(NAZVANIE).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setTitle(OFFER_NAME).setCurrentControlNum(2);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем название, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearName() {
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(OFFER_NAME);
        offerAddSteps.waitSomething(3, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setTitle("");
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Категория», после «Дальше» с экрана добавления названия")
    public void shouldSeeCategoryScreenAfterNext() {
        offerAddSteps.clearAndSetName(OFFER_NAME);
        offerAddSteps.onFormPage().screen(NAZVANIE).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(CATEGORY).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Фотографии» по кнопке «Назад»")
    public void shouldSeeBackToPhotosScreen() {
        offerAddSteps.onFormPage().screen(NAZVANIE).back().click();

        offerAddSteps.onFormPage().screen(PHOTOS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем название, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(OFFER_NAME);
        offerAddSteps.onFormPage().screen(NAZVANIE).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(2).setTitle(OFFER_NAME);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем название, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().input(ENTER_NAME).sendKeys(OFFER_NAME);
        offerAddSteps.onFormPage().screen(NAZVANIE).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем название, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().screen(NAZVANIE).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем название, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().input(ENTER_NAME).clearInput().click();
        offerAddSteps.onFormPage().screen(NAZVANIE).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, не меняя название, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(NAZVANIE).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
