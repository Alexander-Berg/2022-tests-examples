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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.CATEGORY;
import static ru.yandex.general.mobile.page.FormPage.NAZVANIE;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.NO_SUITABLE;
import static ru.yandex.general.mobile.page.FormPage.OPISANIE;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Категория»")
@DisplayName("Тесты на экране «Категория»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepCategoryTest {

    private static final String CHECKED = "_labelChecked";
    private static final String ELEKTRONIKA = "Электроника";
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
        category = PERENOSKA;
        expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(1).setCategoryPreset(GOODS).setPhotos(asList(
                photo().setNamespace(PHOTO_NAMESPACE))).setTitle(category.getCategoryName());
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).withName(category.getCategoryName()).fillToCategoryStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Дальше» без выбранной категории")
    public void shouldNotSeeNextButtonWithoutCheckedCategory() {
        offerAddSteps.onFormPage().screen(CATEGORY).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По дефолту отображается 3 предложенных категории")
    public void shouldSeeThreeSuggestedCategories() {
        offerAddSteps.onFormPage().categories().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмем «Нет подходящей» - отображается 6 категорий")
    public void shouldSeeSixCategoriesAfterNotSuitableClick() {
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();

        offerAddSteps.onFormPage().categories().should(hasSize(6));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет выбранной категории при переходе на экран")
    public void shouldNotSeeCheckedCategory() {
        offerAddSteps.onFormPage().checkedCategory().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По клику на предложенную категорию она становится выбранной")
    public void shouldSeeCheckedCategoryInFirstSuggest() {
        offerAddSteps.onFormPage().categories().get(1).click();

        offerAddSteps.onFormPage().categories().get(1).should(hasClass(containsString(CHECKED)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Дальше» при выборе категории")
    public void shouldSeeNextButtonAfterCategoryClick() {
        offerAddSteps.onFormPage().categories().get(1).click();

        offerAddSteps.onFormPage().screen(CATEGORY).button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Дальше» при выборе категории из попапа ручного выбора")
    public void shouldSeeNextButtonAfterCategoryClickFromManualCategoryChoice() {
        choiceCategoryFromManualScreen();

        offerAddSteps.onFormPage().screen(CATEGORY).button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмем «Нет подходящей», по клику на новую предложенную категорию она становится выбранной")
    public void shouldSeeCheckedCategoryInSecondSuggest() {
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().categories().get(5).click();

        offerAddSteps.onFormPage().categories().get(5).should(hasClass(containsString(CHECKED)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмем на вторую кнопку «Нет подходящей» - открывается экран ручного выбора категории")
    public void shouldSeeManualCategoryChoice() {
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();

        offerAddSteps.onFormPage().screen("Выберите категорию").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("На экране ручного выбора категории выбираем корневую категорию")
    public void shouldSeeManualCategoryScreenChoice() {
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(ELEKTRONIKA).click();

        offerAddSteps.onFormPage().screen(ELEKTRONIKA).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("На экране ручного выбора категории выбираем корневую категорию")
    public void shouldSeeManualCategoryScreenChoiceFinalCategory() {
        choiceCategoryFromManualScreen();

        offerAddSteps.onFormPage().categories().should(hasSize(7));
        offerAddSteps.onFormPage().categories().get(0).should(hasText(containsString("Мобильные телефоны")));
        offerAddSteps.onFormPage().categories().get(0).should(hasClass(containsString(CHECKED)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем экран ручного выбора категории")
    public void shouldSeeManualCategoryScreenClose() {
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().screen("Выберите категорию").close().click();

        offerAddSteps.onFormPage().screen(CATEGORY).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Описание», после «Дальше» с экрана добавления категории")
    public void shouldSeeDescriptionScreenAfterNext() {
        offerAddSteps.onFormPage().categories().get(1).click();
        offerAddSteps.onFormPage().screen(CATEGORY).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(OPISANIE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию из первого саджеста, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterFirstSuggestCategoryClick() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().categorySelect().spanLink(category.getCategoryName()).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCurrentControlNum(2).setCategoryId(category.getCategoryId());
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withRequestText(
                        expectedUpdateDraft.toString())
                .withPathsToBeIgnored(JSONPATHS_TO_IGNORE).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию с экрана ручного выбора, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterManualCategoryChoice() {
        ajaxProxySteps.clearHar();
        choiceCategoryFromManualScreen();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCurrentControlNum(2).setCategoryId("mobilnie-telefoni_OobNbL");
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withRequestText(
                        expectedUpdateDraft.toString())
                .withPathsToBeIgnored(JSONPATHS_TO_IGNORE).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию из первого саджеста, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterNextClick() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().categorySelect().spanLink(category.getCategoryName()).click();
        offerAddSteps.onFormPage().screen(CATEGORY).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCurrentControlNum(3).setCategoryId("transportirovka-perenoski_2HHoms");
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withRequestText(
                        expectedUpdateDraft.toString())
                .withRequestCount(2)
                .withPathsToBeIgnored(JSONPATHS_TO_IGNORE).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Название» по кнопке «Назад»")
    public void shouldSeeBackToTitleScreen() {
        offerAddSteps.onFormPage().screen(CATEGORY).back().click();

        offerAddSteps.onFormPage().screen(NAZVANIE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().categorySelect().spanLink(category.getCategoryName()).click();
        offerAddSteps.onFormPage().screen(CATEGORY).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(3).setCategoryId(category.getCategoryId());
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().categories().get(1).click();
        offerAddSteps.onFormPage().screen(CATEGORY).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().categories().get(1).click();
        offerAddSteps.onFormPage().screen(CATEGORY).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем категорию, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().categories().get(1).click();
        offerAddSteps.onFormPage().screen(CATEGORY).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, не выбирая категорию, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(CATEGORY).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    private void choiceCategoryFromManualScreen() {
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link(ELEKTRONIKA).click();
        offerAddSteps.onFormPage().link("Телефоны и умные часы").click();
        offerAddSteps.onFormPage().link("Мобильные телефоны").click();
    }

}
