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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.NAZVANIE;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.PHOTOS;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Фотографии»")
@DisplayName("Тесты на экране «Фотографии»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepPhotoTest {

    private static final String PHOTO_STEP_TEXT = "Добавьте фото и мы определим название автоматически";
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
        expectedUpdateDraft.getForm().setCurrentControlNum(0).setCategoryPreset(GOODS);
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.fillSection();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается подсказка на экране добавления фото")
    public void shouldSeeHintPhotoStep() {
        offerAddSteps.onFormPage().text().should(hasText(PHOTO_STEP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Дальше» без фото")
    public void shouldNotSeeNextButtonWithoutPhoto() {
        offerAddSteps.onFormPage().screen(PHOTOS).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Дальше» после добавления фото")
    public void shouldSeeNextButtonAfterAddPhoto() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));

        offerAddSteps.onFormPage().screen(PHOTOS).button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("В названии страницы появляется каунтер после добавления фото")
    public void shouldSeeScreenTitleWithPhotoCounter() {
        offerAddSteps.addPhoto();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screenTitle().should(hasText(format("%s: %d", PHOTOS, 1)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("В названии страницы пропадает каунтер после удаления фото")
    public void shouldSeeScreenTitleWithoutPhotoCounter() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().screenTitle().waitUntil(hasText(format("%s: %d", PHOTOS, 1)));
        offerAddSteps.onFormPage().photoList().get(0).delete().click();

        offerAddSteps.onFormPage().screenTitle().should(hasText(PHOTOS));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подсказка на экране добавления фото не пропадает после добавления фото")
    public void shouldSeeHintPhotoStepAfterAddPhoto() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));

        offerAddSteps.onFormPage().text().should(hasText(PHOTO_STEP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пропадает фотография при её удалении")
    public void shouldSeeNoPhotoAfterDelete() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().photoList().get(0).delete().click();

        offerAddSteps.onFormPage().photoList().should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Пропадает кнопка «Назад» после удаления фотографии")
    public void shouldSeeNoNextButtonAfterDeletePhoto() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().photoList().get(0).delete().click();

        offerAddSteps.onFormPage().screen(PHOTOS).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterAddPhoto() {
        offerAddSteps.addPhoto();
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));

        expectedUpdateDraft.getForm().setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)));

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, проверяем отправку /updateDraft после нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterAddPhotoAndNext() {
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().screen(PHOTOS).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(1).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)));

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем фото, проверяем отправку /updateDraft после удаления")
    public void shouldSeeUpdateDraftAfterDeletePhoto() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().photoList().get(0).delete().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setCurrentControlNum(0).setCategoryPreset(GOODS).setPhotos(asList());

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Название», после «Дальше» с экрана добавления фото")
    public void shouldSeeTitleScreenAfterNext() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().screen(PHOTOS).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(NAZVANIE).should(isDisplayed());
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран пресетов по кнопке «Назад»")
    public void shouldSeeBackToPresetScreen() {
        offerAddSteps.onFormPage().screen(PHOTOS).back().click();

        offerAddSteps.onFormPage().h1().should(hasText("Выберите категорию"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().screen(PHOTOS).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(1).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().screen(PHOTOS).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().screen(PHOTOS).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фото, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.onFormPage().screen(PHOTOS).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем фото, закрываем форму, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(PHOTOS).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
