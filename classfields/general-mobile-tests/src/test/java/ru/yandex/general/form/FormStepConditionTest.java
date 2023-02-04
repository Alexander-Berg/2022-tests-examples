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
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.HARAKTERISTIKI;
import static ru.yandex.general.mobile.page.FormPage.NEW_PRODUCT;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.mobile.page.FormPage.SOSTOYANIE;
import static ru.yandex.general.mobile.page.FormPage.USED;
import static ru.yandex.general.mobile.page.FormPage.VIDEO;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.NEW_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.NULL_STRING;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Состояние»")
@DisplayName("Тесты на экране «Состояние»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepConditionTest {

    private static final String CONDITION_TEXT = "Давайте по-честному. Новый — это совсем не использованный. А ещё лучше, если сохранились бирки и упаковка.";
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
        expectedUpdateDraft.getForm().setCurrentControlNum(6)
                .setCategoryPreset(GOODS).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)))
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle());
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).withName(category.getTitle()).fillToConditionStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст на экране состояния")
    public void shouldSeeConditionScreenText() {
        offerAddSteps.onFormPage().paragraph().should(hasText(CONDITION_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По дефолту не выбрано состояние")
    public void shouldSeeNoSelectedCondition() {
        offerAddSteps.onFormPage().selectedCondition().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Дальше», без выбранного состояния")
    public void shouldNotSeeNextWithoutCondition() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на «Новый товар», кнопка становится выбранной")
    public void shouldSeeSelectedConditionNew() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();

        offerAddSteps.onFormPage().selectedCondition().should(hasText(NEW_PRODUCT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на «Уже использовался», кнопка становится выбранной")
    public void shouldSeeSelectedConditionUsed() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(USED).click();

        offerAddSteps.onFormPage().selectedCondition().should(hasText(USED));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Характеристики» по клику на «Новый товар»")
    public void shouldSeeAttributesScreenAfterSelectConditionNew() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Характеристики» по клику на «Уже использовался»")
    public void shouldSeeAttributesScreenAfterSelectConditionUsed() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(USED).click();

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на «Новый товар», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClickConditionNew() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCondition(NEW_CONDITION);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на «Уже использовался», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClickConditionUsed() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(USED).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCondition(USED_CONDITION);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на «Новый товар», затем меняем на «Уже пользовался», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChangeConditionNewToUsed() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).back().waitUntil(isDisplayed()).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(USED).click();

        expectedUpdateDraft.getForm().setCondition(USED_CONDITION);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на «Уже пользовался», затем меняем на «Новый товар», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterChangeConditionUsedToNew() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(USED).click();
        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).back().waitUntil(isDisplayed()).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();

        expectedUpdateDraft.getForm().setCondition(NEW_CONDITION);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Видео» по кнопке «Назад»")
    public void shouldSeeBackToVideoScreen() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).back().click();

        offerAddSteps.onFormPage().screen(VIDEO).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем состояние, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(USED).click();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCondition(USED_CONDITION).setCurrentControlNum(10);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем состояние, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем состояние, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем состояние, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).spanLink(NEW_PRODUCT).click();
        offerAddSteps.onFormPage().screen(SOSTOYANIE).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не выбираем состояние, закрываем форму, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(SOSTOYANIE).close().click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
