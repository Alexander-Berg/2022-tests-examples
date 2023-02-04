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
import ru.yandex.general.consts.FormConstants;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.HARAKTERISTIKI;
import static ru.yandex.general.mobile.page.FormPage.SKIP;
import static ru.yandex.general.mobile.page.FormPage.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Видео»")
@DisplayName("Тесты на экране «Видео»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepVideoNextFromWorkCategoriesTest {

    private FormConstants.Categories category;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Характеристики», после «Пропустить» с экрана добавления видео. Категория резюме.")
    public void shouldSeeAttributesScreenAfterNextRezumeCategory() {
        category = FormConstants.Categories.REZUME_IN_SELLING;
        offerAddSteps.withCategory(category).fillToVideoStep();
        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Характеристики», после «Пропустить» с экрана добавления видео. Категория вакансии.")
    public void shouldSeeAttributesScreenAfterNextVakanciiCategory() {
        category = FormConstants.Categories.VAKANCIYA_MENEGER;
        offerAddSteps.withCategory(category).fillToVideoStep();
        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается экран «Характеристики», после «Пропустить» с экрана добавления видео. Категория услуги.")
    public void shouldSeeAttributesScreenAfterNextUslugiCategory() {
        category = FormConstants.Categories.USLUGI_DOSTAVKI;
        offerAddSteps.withCategory(category).fillToVideoStep();
        offerAddSteps.onFormPage().screen(VIDEO).button(SKIP).click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).should(isDisplayed());
    }

}
