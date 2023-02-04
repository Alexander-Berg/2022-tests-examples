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

import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.PHOTOS;
import static ru.yandex.general.mobile.page.FormPage.VESCHI;
import static ru.yandex.general.page.FormPage.PARTS;
import static ru.yandex.general.page.FormPage.RABOTA;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран выбора пресета")
@DisplayName("Тесты на экран выбора пресета")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepPresetsTest {

    private static final String[] JSONPATHS_TO_IGNORE = {"draftId"};

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
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем пресет «Вещи», проверяем отправку /updateDraft")
    public void shouldSeePresetChoiceGoodsUpdateDraftRequest() {
        offerAddSteps.onFormPage().section(VESCHI).click();

        UpdateDraft expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(0).setCategoryPreset("Goods");

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем пресет «Работа», проверяем отправку /updateDraft")
    public void shouldSeePresetChoiceWorkUpdateDraftRequest() {
        offerAddSteps.onFormPage().section(RABOTA).click();

        UpdateDraft expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(0).setCategoryPreset("Rabota");

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем пресет «Транспорт и запчасти», проверяем отправку /updateDraft")
    public void shouldSeePresetChoicePartsUpdateDraftRequest() {
        offerAddSteps.onFormPage().section(PARTS).click();

        UpdateDraft expectedUpdateDraft = getUpdateDraftTemplate();
        expectedUpdateDraft.getForm().setCurrentControlNum(0).setCategoryPreset("Parts");

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Фотографии», после выбора пресета «Вещи»")
    public void shouldSeePhotoScreenAfterGoodsChoice() {
        offerAddSteps.onFormPage().section(VESCHI).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(PHOTOS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Фотографии», после выбора пресета «Работа»")
    public void shouldSeePhotoScreenAfterWorkChoice() {
        offerAddSteps.onFormPage().section(RABOTA).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(PHOTOS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Фотографии», после выбора пресета «Транспорт и запчасти»")
    public void shouldSeePhotoScreenAfterPartsChoice() {
        offerAddSteps.onFormPage().section(PARTS).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(PHOTOS).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, не выбирая пресет, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        offerAddSteps.onFormPage().close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

}
