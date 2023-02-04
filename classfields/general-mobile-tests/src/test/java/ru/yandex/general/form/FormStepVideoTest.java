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
import static ru.yandex.general.beans.card.Video.video;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.Input.PLACEHOLDER;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.mobile.page.FormPage.SKIP;
import static ru.yandex.general.mobile.page.FormPage.SOSTOYANIE;
import static ru.yandex.general.mobile.page.FormPage.VIDEO;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Видео»")
@DisplayName("Тесты на экране «Видео»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepVideoTest {

    private static final String VIDEO_PLACEHOLDER = "Ссылка на YouTube";
    private static final String TEXT = "Одно видео лучше тысячи фотографий!";
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=UPG-oOZyxME";
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
        expectedUpdateDraft.getForm().setCurrentControlNum(4)
                .setCategoryPreset(GOODS).setPhotos(asList(photo().setNamespace(PHOTO_NAMESPACE)))
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle());
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
        offerAddSteps.withCategory(category).withName(category.getTitle()).fillToVideoStep();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Плейсхолдер инпута видео")
    public void shouldSeeVideoInputPlaceholder() {
        offerAddSteps.onFormPage().screen(VIDEO).input().should(hasAttribute(PLACEHOLDER, VIDEO_PLACEHOLDER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст на экране видео")
    public void shouldSeeVideoScreenText() {
        offerAddSteps.onFormPage().paragraph().should(hasText(TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Пропустить» без видео")
    public void shouldSeeSkipButtonWithoutVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается картинка ютуба без видео")
    public void shouldSeeYoutubeImageWithoutVideo() {
        offerAddSteps.onFormPage().youtubeImage().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Дальше» с видео")
    public void shouldSeeNextButtonWithVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);

        offerAddSteps.onFormPage().screen(VIDEO).button(NEXT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается картинка ютуба с видео")
    public void shouldNotSeeYoutubeImageWithVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);

        offerAddSteps.onFormPage().youtubeImage().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Пропустить» с видео")
    public void shouldNotSeeSkipButtonWithVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);

        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается видео фрейм с видео")
    public void shouldSeeVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);

        offerAddSteps.onFormPage().videoFrame().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается картинка ютуба после очистки инпута ссылки на видео")
    public void shouldSeeYoutubeImageAfterClearVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().videoFrame().waitUntil(isDisplayed());
        offerAddSteps.onFormPage().screen(VIDEO).input().clearInput().click();

        offerAddSteps.onFormPage().youtubeImage().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кнопка «Пропустить» после очистки инпута ссылки на видео")
    public void shouldSeeSkipButtonAfterClearVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().videoFrame().waitUntil(isDisplayed());
        offerAddSteps.onFormPage().screen(VIDEO).input().clearInput().click();

        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается видео фрейм после очистки инпута ссылки на видео")
    public void shouldNotSeeVideoFrameAfterClearVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().videoFrame().waitUntil(isDisplayed());
        offerAddSteps.onFormPage().screen(VIDEO).input().clearInput().click();

        offerAddSteps.onFormPage().videoFrame().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Дальше» после очистки инпута ссылки на видео")
    public void shouldSeeNoNextButtonAfterClearVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().videoFrame().waitUntil(isDisplayed());
        offerAddSteps.onFormPage().screen(VIDEO).input().clearInput().click();

        offerAddSteps.onFormPage().screen(VIDEO).button(NEXT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем видео, проверяем отправку /updateDraft без нажатия на «Далее»")
    public void shouldSeeUpdateDraftAfterAddVideo() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setVideo(video().setUrl(VIDEO_URL));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем видео, жмем «Далее», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterAddVideoAndNext() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().screen(VIDEO).button(NEXT).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setVideo(video().setUrl(VIDEO_URL)).setCurrentControlNum(5);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем видео, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterClearVideo() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.waitSomething(3, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(VIDEO).input().clearInput().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setVideo(video().setUrl(""));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем видео, жмем «Пропустить», проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftAfterSkipVideoAndSkip() {
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        expectedUpdateDraft.getForm().setVideo(video().setUrl("")).setCurrentControlNum(5);
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Состояние», после «Дальше» с экрана добавления видео. Товарная категория.")
    public void shouldSeeConditionScreenAfterNextGoodsCategory() {
        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(SOSTOYANIE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем видео, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().screen(VIDEO).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(5).setVideo(video().setUrl(VIDEO_URL));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем видео, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().screen(VIDEO).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем видео, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().screen(VIDEO).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем видео, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        offerAddSteps.onFormPage().screen(VIDEO).input().sendKeys(VIDEO_URL);
        offerAddSteps.onFormPage().screen(VIDEO).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем видео, закрываем форму, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().screen(VIDEO).close().click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
