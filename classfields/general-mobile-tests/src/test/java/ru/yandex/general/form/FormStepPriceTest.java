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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Photo.photo;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Price.price;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.PriceInCurrency.priceInCurrency;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Salary.salary;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.Input.DISABLED;
import static ru.yandex.general.mobile.element.Input.PLACEHOLDER;
import static ru.yandex.general.mobile.page.BasePage.MAIN_PAGE_MOSCOW_H1;
import static ru.yandex.general.mobile.page.FormPage.CENA_R;
import static ru.yandex.general.mobile.page.FormPage.HARAKTERISTIKI;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.RESET;
import static ru.yandex.general.mobile.page.FormPage.SAVE_DRAFT;
import static ru.yandex.general.mobile.page.FormPage.SKIP;
import static ru.yandex.general.mobile.page.FormPage.ZARPLATA;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_DRAFT;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.general.step.OfferAddSteps.GOODS;
import static ru.yandex.general.step.OfferAddSteps.NULL_STRING;
import static ru.yandex.general.step.OfferAddSteps.PHOTO_NAMESPACE;
import static ru.yandex.general.step.OfferAddSteps.RABOTA;
import static ru.yandex.general.step.OfferAddSteps.USED_CONDITION;
import static ru.yandex.general.step.OfferAddSteps.getUpdateDraftTemplate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран цены")
@DisplayName("Тесты на экране цены")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormStepPriceTest {

    private static final String MY_PRICE = "Моя цена";
    private static final String IN_MONTH = "В месяц";
    private static final String BIG_PRICE = "99000000";
    private static final String SALLARY = "55000";
    private static final String MINIMUM_PRICE = "1";
    private static final String MIDDLE_PRICE = "1000";
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
        expectedUpdateDraft.getForm().setCurrentControlNum(11).setPhotos(asList(
                photo().setNamespace(PHOTO_NAMESPACE)));
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается плейсхолдер инпута цены. Товарная категория.")
    public void shouldSeePriceScreenInputPlaceholderGoodsCategory() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(CENA_R).input().should(hasAttribute(PLACEHOLDER, formatWithRubles(MY_PRICE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается плейсхолдер инпута зарплаты. Категория вакансии.")
    public void shouldSeePriceScreenInputPlaceholderRezumeCategory() {
        category = FormConstants.Categories.REZUME_IN_SELLING;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(ZARPLATA).input().should(hasAttribute(PLACEHOLDER, formatWithRubles(IN_MONTH)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается плейсхолдер инпута зарплаты. Категория резюме.")
    public void shouldSeePriceScreenInputPlaceholderVakanciiCategory() {
        category = FormConstants.Categories.VAKANCIYA_MENEGER;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(ZARPLATA).input().should(hasAttribute(PLACEHOLDER, formatWithRubles(IN_MONTH)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается плейсхолдер инпута цены. Категория услуги.")
    public void shouldSeePriceScreenInputPlaceholderUslugiCategory() {
        category = FormConstants.Categories.USLUGI_DOSTAVKI;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(CENA_R).input().should(hasAttribute(PLACEHOLDER, formatWithRubles(MY_PRICE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается свитчер «Отдам даром». Товарная категория.")
    public void shouldSeeFreeSwitcherGoodsCategory() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(CENA_R).switcher().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается свитчер «Отдам даром». Категория вакансии.")
    public void shouldNotSeeFreeSwitcherRezumeCategory() {
        category = FormConstants.Categories.REZUME_IN_SELLING;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(ZARPLATA).switcher().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается свитчер «Отдам даром». Категория резюме.")
    public void shouldNotSeeFreeSwitcherVakanciiCategory() {
        category = FormConstants.Categories.VAKANCIYA_MENEGER;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(ZARPLATA).switcher().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается свитчер «Отдам даром». Категория услуги.")
    public void shouldNotSeeFreeSwitcherUslugiCategory() {
        category = FormConstants.Categories.USLUGI_DOSTAVKI;
        offerAddSteps.withCategory(category).fillToPriceStep();

        offerAddSteps.onFormPage().screen(CENA_R).switcher().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем цену. Товарная категория. Проверяем отправку /updateDraft без нажатия «Далее»")
    public void shouldSeePriceGoodsCategoryUpdateDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(BIG_PRICE);

        expectedUpdateDraft.getForm().setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setPriceInCurrency(priceInCurrency(BIG_PRICE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем зарплату. Категория резюме. Проверяем отправку /updateDraft без нажатия «Далее»")
    public void shouldSeePriceRezumeCategoryUpdateDraft() {
        category = FormConstants.Categories.REZUME_IT;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(ZARPLATA).input().sendKeys(BIG_PRICE);

        expectedUpdateDraft.getForm().setCategoryPreset(RABOTA)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setPrice(price().setSalary(salary(BIG_PRICE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем зарплату. Категория вакансии. Проверяем отправку /updateDraft без нажатия «Далее»")
    public void shouldSeePriceVakanciiCategoryUpdateDraft() {
        category = FormConstants.Categories.VAKANCIYA_RABOCHII;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(ZARPLATA).input().sendKeys(SALLARY);

        expectedUpdateDraft.getForm().setCategoryPreset(RABOTA)
                .setCurrentControlNum(8)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setPrice(price().setSalary(salary(SALLARY)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем цену. Категория услуг. Проверяем отправку /updateDraft без нажатия «Далее»")
    public void shouldSeePriceUslugiCategoryUpdateDraft() {
        category = FormConstants.Categories.USLUGI_DOSTAVKI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MINIMUM_PRICE);

        expectedUpdateDraft.getForm().setCategoryPreset(RABOTA)
                .setCurrentControlNum(7)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setPrice(price().setPriceInCurrency(priceInCurrency(MINIMUM_PRICE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем цену, жмём на  «Далее». Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeePriceGoodsCategoryUpdateDraftAfterNext() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(12).setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setPriceInCurrency(priceInCurrency(MIDDLE_PRICE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем «Отдам даром». Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeeFreePriceGoodsCategoryUpdateDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();

        expectedUpdateDraft.getForm().setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setFree(true));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем «Отдам даром», убираем «Отдам даром». Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeeNoFreePriceGoodsCategoryUpdateDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();

        expectedUpdateDraft.getForm().setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setFree(false));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем «Отдам даром», жмём на  «Далее». Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeeFreePriceGoodsCategoryUpdateDraftAfterNext() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();
        offerAddSteps.onFormPage().screen(CENA_R).button(NEXT).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(12).setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setFree(true));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем цену, жмём «Пропустить». Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeeNoPriceGoodsCategoryUpdateDraftAfterSkip() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).button(SKIP).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(12).setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setFree(false));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем зарплату, жмём «Пропустить». Категория резюме. Проверяем отправку /updateDraft")
    public void shouldSeeNoPriceRezumeCategoryUpdateDraftAfterSkip() {
        category = FormConstants.Categories.REZUME_IT;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(ZARPLATA).button(SKIP).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(12).setCategoryPreset(RABOTA)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setPrice(price().setFree(false).setSalaryReplace(NULL_STRING));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString().replace("salaryReplace", "salary"))
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем зарплату, жмём «Пропустить». Категория вакансии. Проверяем отправку /updateDraft")
    public void shouldSeeNoPriceVakanciiCategoryUpdateDraftAfterSkip() {
        category = FormConstants.Categories.VAKANCIYA_RABOCHII;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(ZARPLATA).button(SKIP).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(9).setCategoryPreset(RABOTA)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setPrice(price().setFree(false).setSalaryReplace(NULL_STRING));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString().replace("salaryReplace", "salary"))
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не добавляем цену, жмём «Пропустить». Категория услуг. Проверяем отправку /updateDraft")
    public void shouldSeeNoPriceUslugiCategoryUpdateDraftAfterSki() {
        category = FormConstants.Categories.USLUGI_DOSTAVKI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).button(SKIP).click();

        expectedUpdateDraft.getForm().setCategoryPreset(RABOTA)
                .setCurrentControlNum(8)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle())
                .setPrice(price().setFree(false));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливаем «Цена даром», отменяем, добавляем цену. Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeeFreePriceAndSetPriceGoodsCategoryUpdateDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);

        expectedUpdateDraft.getForm().setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setPriceInCurrency(priceInCurrency(MIDDLE_PRICE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем цену, затем устанавливаем «Цена даром». Товарная категория. Проверяем отправку /updateDraft")
    public void shouldSeeSetPriceAndSetFreePriceGoodsCategoryUpdateDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();

        expectedUpdateDraft.getForm().setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setFree(true));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Инпут цены дизейблится при выборе «Отдам даром». Товарная категория.")
    public void shouldSeePriceScreenInputDisabledWithFreePriceGoodsCategory() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();

        offerAddSteps.onFormPage().screen(CENA_R).input().should(hasAttribute(DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Инпут цены раздизейбливается при отключении «Отдам даром». Товарная категория.")
    public void shouldSeePriceScreenInputActivatedWithCancelFreePriceGoodsCategory() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();
        offerAddSteps.onFormPage().screen(CENA_R).input().waitUntil(hasAttribute(DISABLED, TRUE));
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();

        offerAddSteps.onFormPage().screen(CENA_R).input().should(not(hasAttribute(DISABLED, TRUE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Инпут цены очищается при выборе «Отдам даром». Товарная категория.")
    public void shouldSeePriceScreenInputClearedWithFreePriceGoodsCategory() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).switcher().click();

        offerAddSteps.onFormPage().screen(CENA_R).input().should(hasValue(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на экран «Характеристики» по кнопке «Назад»")
    public void shouldSeeBackToTitleScreen() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).back().click();

        offerAddSteps.onFormPage().screen(HARAKTERISTIKI).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим цену, закрываем форму сохраняя черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormSaveDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        expectedUpdateDraft.getForm().setCurrentControlNum(12).setCategoryPreset(GOODS)
                .setCategoryId(category.getCategoryId())
                .setTitle(category.getTitle()).setCondition(USED_CONDITION)
                .setPrice(price().setPriceInCurrency(priceInCurrency(MIDDLE_PRICE)));
        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(expectedUpdateDraft.toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим цену, закрываем форму сбрасывая черновик, проверяем отправку /updateDraft")
    public void shouldSeeUpdateDraftByCloseFormResetDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).close().click();
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(UPDATE_DRAFT).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getUpdateDraftTemplate().toString())
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим цену, закрываем форму сохраняя черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormSaveDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).close().click();
        offerAddSteps.onFormPage().popup().button(SAVE_DRAFT).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим цену, закрываем форму сбрасывая черновик, проверяем что оказались на главной")
    public void shouldSeeCloseFormResetDraft() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).input().sendKeys(MIDDLE_PRICE);
        offerAddSteps.onFormPage().screen(CENA_R).close().click();
        offerAddSteps.onFormPage().popup().button(RESET).waitUntil(isDisplayed()).click();

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрываем форму, не вводя цену, проверяем что оказались на главной")
    public void shouldSeeCloseForm() {
        category = FormConstants.Categories.AKUSTICHESKIE_SISTEMI;
        offerAddSteps.withCategory(category).fillToPriceStep();
        offerAddSteps.onFormPage().screen(CENA_R).close().click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);

        offerAddSteps.onListingPage().h1().should(hasText(MAIN_PAGE_MOSCOW_H1));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    private String formatWithRubles(String text) {
        return format("%s, ₽", text);
    }

}
