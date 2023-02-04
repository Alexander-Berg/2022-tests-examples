package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.Option;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.card.Attribute;
import ru.yandex.general.beans.card.Card;
import ru.yandex.general.consts.FormConstants;
import ru.yandex.general.mobile.page.FormPage;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.general.beans.Attribute.input;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.consts.Attributes.age;
import static ru.yandex.general.consts.Attributes.education;
import static ru.yandex.general.consts.Attributes.sex;
import static ru.yandex.general.consts.Attributes.workSchedule;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Conditions.NEW;
import static ru.yandex.general.consts.FormConstants.Conditions.USED;
import static ru.yandex.general.consts.GeneralFeatures.DRAFT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.CATEGORY;
import static ru.yandex.general.mobile.page.FormPage.CENA;
import static ru.yandex.general.mobile.page.FormPage.COMPLETE;
import static ru.yandex.general.mobile.page.FormPage.ADRES;
import static ru.yandex.general.mobile.page.FormPage.NAZVANIE;
import static ru.yandex.general.mobile.page.FormPage.NO_SUITABLE;
import static ru.yandex.general.mobile.page.FormPage.OPISANIE;
import static ru.yandex.general.mobile.page.FormPage.PHOTOS;
import static ru.yandex.general.mobile.page.FormPage.PUBLISH;
import static ru.yandex.general.mobile.page.FormPage.SOSTOYANIE;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.general.page.FormPage.FIRST;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(DRAFT_FEATURE)
@DisplayName("Редактирование черновика")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class DraftTest {

    private static final String NAME_EDITED = "Переноска для кота";
    private static final String DESCRIPTION = "Описание, несколько слов + символы";
    private static final String DESCRIPTION_EDITED = "Отредактированное описание";
    private static final String PRICE = "2499";
    private static final String PRICE_EDITED = "13200";
    private static final String ADDRESS = "проспект Карла Маркса, 2";
    private static final String SECOND_ADDRESS = "ул. Ленина, 12";
    private static final String PARENT_CATEGORY_EDITED = "Электроника";
    private static final String WORK_SCHEDULE = "График работы";
    private static final String EDUCATION = "Образование";
    private static final String WORK_EXPIRENCE = "Опыт работы";
    private static final String SEX = "Пол";
    private static final String TYPE_OF_EMPLOYMENT = "Тип занятости";
    private static final String AGE = "Возраст от 14 до 99 ";
    private static final String AGE_VALUE = "23";
    private static final String EDUCATION_VALUE = "Высшее";
    private static final String WORK_SCHEDULE_VALUE = "полный день";
    private static final String WORK_EXPIRENCE_VALUE = "Более 3 лет";
    private static final String SEX_VALUE = "Женский";
    private static final String TYPE_OF_EMPLOYMENT_VALUE = "Полная занятость";
    private static final String CHANGE = "Изменить";

    private FormConstants.Categories category;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private GraphqlSteps graphqlSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возврат на черновик на этапе заполнения адреса")
    public void shouldReturnToDraftOnAddressStep() {
        category = PERENOSKA;
        offerAddSteps.withCategory(category)
                .withName(category.getTitle())
                .withAddress(ADDRESS)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToContactsStep();
        urlSteps.open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.stepButtonClick();
        offerAddSteps.fillAddress();
        offerAddSteps.fillDelivery();
        offerAddSteps.finalStep(PUBLISH);
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(category.getTitle());
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo(NEW.getValue());
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(ADDRESS);
            s.assertThat(offerCard.getDescription()).as("Описание").isEqualTo(DESCRIPTION);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(category.getCategoryName());
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(category.getParentCategory());
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование основных полей черновика")
    public void shouldChangeBasicFieldsOnDraft() {
        category = PERENOSKA;
        offerAddSteps.withCategory(category)
                .withAddress(ADDRESS)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToFinalStep();
        urlSteps.open();
        offerAddSteps.onFormPage().button(CONTINUE).hover();

        offerAddSteps.onFormPage().draftSection(NAZVANIE).input().clearInput().click();
        offerAddSteps.onFormPage().draftSection(NAZVANIE).input().sendKeys(NAME_EDITED);
        offerAddSteps.onFormPage().draftSection(OPISANIE).clearTextarea().click();
        offerAddSteps.onFormPage().draftSection(OPISANIE).textarea().sendKeys(DESCRIPTION_EDITED);
        offerAddSteps.onFormPage().draftSection(CENA).input().clearInput().click();
        offerAddSteps.onFormPage().draftSection(CENA).input().sendKeys(PRICE_EDITED);
        offerAddSteps.onFormPage().draftSection(SOSTOYANIE).spanLink(FormPage.USED).click();
        offerAddSteps.onFormPage().draftSection(ADRES).input().click();
        offerAddSteps.onFormPage().wrapper("Первый адрес").input().click();
        offerAddSteps.onFormPage().wrapper(ADRES).clearTextarea().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().sendKeys(SECOND_ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(SECOND_ADDRESS).click();
        offerAddSteps.onFormPage().button(COMPLETE).click();

        offerAddSteps.finalStep(PUBLISH);
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE_EDITED);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(NAME_EDITED);
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo(USED.getValue());
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(SECOND_ADDRESS);
            s.assertThat(offerCard.getDescription()).as("Описание").isEqualTo(DESCRIPTION_EDITED);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(category.getCategoryName());
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(category.getParentCategory());
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории черновика через саджест")
    public void shouldChangeCategoryOnDraft() {
        category = PERENOSKA;
        offerAddSteps.withCategory(category)
                .withAddress(ADDRESS)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToFinalStep();
        urlSteps.open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.scrollToTop();

        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(CHANGE).click();
        offerAddSteps.onFormPage().categorySelect().spanLink("Люльки и переноски").click();

        offerAddSteps.finalStep(PUBLISH);
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Люльки и переноски");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo("Детские товары");
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории черновика через ручной поиск")
    public void shouldChangeCategoryManualOnDraft() {
        category = PERENOSKA;
        offerAddSteps.withCategory(category)
                .withAddress(ADDRESS)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToFinalStep();

        urlSteps.open();

        offerAddSteps.onFormPage().draftSection(PHOTOS).hover();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link("Электроника").waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().link("Телефоны и умные часы").click();
        offerAddSteps.onFormPage().link("Мобильные телефоны").click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        offerAddSteps.onFormPage().button(CONTINUE).waitUntil(isDisplayed()).click();
        offerAddSteps.finalStep(PUBLISH);
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Мобильные телефоны");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Электроника")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории черновика с сохранением атрибутов")
    public void shouldEditCategoryWithAttributesSaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withAttributes(
                select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                select(EDUCATION).setValue(EDUCATION_VALUE),
                select(SEX).setValue(SEX_VALUE),
                input(AGE).setValue(AGE_VALUE))
                .fillToFinalStep();

        urlSteps.open();

        offerAddSteps.onFormPage().draftSection(PHOTOS).hover();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().link("Резюме").click();
        offerAddSteps.onFormPage().link("Продажи").click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        offerAddSteps.onFormPage().button(CONTINUE).waitUntil(isDisplayed()).click();
        offerAddSteps.finalStep(PUBLISH);
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());


        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                age().withInputValue(AGE_VALUE),
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE),
                sex().withSelectValue(SEX_VALUE),
                education().withSelectValue(EDUCATION_VALUE));

        assertThat("Ответы должны совпадать", offerCard.getAttributes(), jsonEquals(attributes)
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории черновика с частичным сохранением атрибутов")
    public void shouldEditCategoryWithAttributesPartialySaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withAttributes(
                select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                select(EDUCATION).setValue(EDUCATION_VALUE),
                select(SEX).setValue(SEX_VALUE),
                input(AGE).setValue(AGE_VALUE))
                .fillToFinalStep();

        urlSteps.open();

        offerAddSteps.onFormPage().draftSection(PHOTOS).hover();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(CATEGORY).link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().link("Работа").click();
        offerAddSteps.onFormPage().link("Вакансии").click();
        offerAddSteps.onFormPage().link("Продажи").click();
        offerAddSteps.onFormPage().link("Менеджер").click();
        offerAddSteps.waitSomething(1, TimeUnit.SECONDS);
        offerAddSteps.onFormPage().button(CONTINUE).waitUntil(isDisplayed()).click();
        offerAddSteps.finalStep(PUBLISH);
        offerAddSteps.onOfferCardPage().successPublishMessage().waitUntil(isDisplayed());


        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE));

        assertThat("Ответы должны совпадать", offerCard.getAttributes(), jsonEquals(attributes)
                .when(Option.IGNORING_ARRAY_ORDER));
    }

}
