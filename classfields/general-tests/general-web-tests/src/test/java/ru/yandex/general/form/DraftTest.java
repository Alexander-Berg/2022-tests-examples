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
import ru.yandex.general.consts.FormConstants.Categories;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.OfferAddSteps;
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
import static ru.yandex.general.consts.Attributes.typeOfEmployment;
import static ru.yandex.general.consts.Attributes.workExpirence;
import static ru.yandex.general.consts.Attributes.workSchedule;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Conditions.NEW;
import static ru.yandex.general.consts.GeneralFeatures.DRAFT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.ADD_MORE_ADDRESS;
import static ru.yandex.general.page.FormPage.CONTINUE;
import static ru.yandex.general.page.FormPage.FIRST;
import static ru.yandex.general.page.FormPage.NAZVANIE;
import static ru.yandex.general.page.FormPage.NEXT;
import static ru.yandex.general.page.FormPage.NO_SUITABLE;
import static ru.yandex.general.page.FormPage.OPISANIE;
import static ru.yandex.general.page.FormPage.ADRES;
import static ru.yandex.general.page.FormPage.USED;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(DRAFT_FEATURE)
@DisplayName("Редактирование черновика оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class DraftTest {

    private static final String NAME_EDITED = "Переноска для кота";
    private static final String DESCRIPTION = "Описание, несколько слов + символы";
    private static final String DESCRIPTION_EDITED = "Отредактированное описание";
    private static final String PRICE = "2499";
    private static final String PRICE_EDITED = "13200";
    private static final String ADDRESS = "проспект Карла Маркса, 2";
    private static final String PARENT_CATEGORY_EDITED = "Электроника";
    private static final String WORK_SCHEDULE = "График работы";
    private static final String EDUCATION = "Образование";
    private static final String WORK_EXPIRENCE = "Опыт работы";
    private static final String SEX = "Пол";
    private static final String TYPE_OF_EMPLOYMENT = "Тип занятости";
    private static final String AGE = "Возраст";
    private static final String AGE_VALUE = "23";
    private static final String EDUCATION_VALUE = "Высшее";
    private static final String WORK_SCHEDULE_VALUE = "полный день";
    private static final String WORK_EXPIRENCE_VALUE = "более 3 лет";
    private static final String SEX_VALUE = "Женский";
    private static final String TYPE_OF_EMPLOYMENT_VALUE = "Полная занятость";

    private Categories category;

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
    @DisplayName("Редактирование основных полей черновика")
    public void shouldChangeBasicFieldsOnDraft() {
        category = PERENOSKA;
        offerAddSteps.withCategory(category)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToAddressStep();
        saveDraftAndReturnToForm();

        offerAddSteps.onFormPage().field(NAZVANIE).input().clearInput().click();
        offerAddSteps.onFormPage().field(NAZVANIE).input().sendKeys(NAME_EDITED);
        offerAddSteps.onFormPage().field(OPISANIE).clearTextarea().click();
        offerAddSteps.onFormPage().field(OPISANIE).textarea().sendKeys(DESCRIPTION_EDITED);
        offerAddSteps.onFormPage().price().clearInput().click();
        offerAddSteps.onFormPage().price().sendKeys(PRICE_EDITED);
        offerAddSteps.onFormPage().condition(USED).click();
        offerAddSteps.onFormPage().button(NEXT).click();
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        offerAddSteps.onFormPage().field(ADRES).input().sendKeys(ADDRESS);
        offerAddSteps.onFormPage().addressesSuggestList().get(FIRST).click();
        offerAddSteps.publish();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE_EDITED);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(NAME_EDITED);
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo("Used");
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(ADDRESS);
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
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToAddressStep();
        saveDraftAndReturnToForm();

        offerAddSteps.onFormPage().field("Категория").link("Изменить").click();
        offerAddSteps.onFormPage().categorySelect().spanLink("Люльки и переноски").click();

        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        offerAddSteps.onFormPage().field(ADRES).input().sendKeys(ADDRESS);
        offerAddSteps.onFormPage().addressesSuggestList().get(FIRST).click();
        offerAddSteps.publish();

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
    public void shouldChangeCategoryInPopupOnDraft() {
        category = PERENOSKA;
        offerAddSteps.withCategory(category)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .fillToAddressStep();
        saveDraftAndReturnToForm();

        offerAddSteps.onFormPage().field("Категория").link("Изменить").click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.onFormPage().modal().link("Электроника").waitUntil(isDisplayed()).click();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().modal().link("Телефоны и умные часы").click();
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        offerAddSteps.onFormPage().modal().link("Мобильные телефоны").click();
        offerAddSteps.nextClick();

        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        offerAddSteps.onFormPage().field(ADRES).input().sendKeys(ADDRESS);
        offerAddSteps.onFormPage().addressesSuggestList().get(FIRST).click();
        offerAddSteps.publish();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Мобильные телефоны");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории черновика с сохранением атрибутов")
    public void shouldEditCategoryWithAttributesSaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .withAttributes(
                        select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                        select(EDUCATION).setValue(EDUCATION_VALUE),
                        select(WORK_EXPIRENCE).setValue(WORK_EXPIRENCE_VALUE),
                        select(SEX).setValue(SEX_VALUE),
                        select(TYPE_OF_EMPLOYMENT).setValue(TYPE_OF_EMPLOYMENT_VALUE),
                        input(AGE).setValue(AGE_VALUE))
                .fillToAddressStep();

        saveDraftAndReturnToForm();

        offerAddSteps.onFormPage().field("Категория").link("Изменить").click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Резюме").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Продажи").click();
        offerAddSteps.onFormPage().button(NEXT).click();
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        offerAddSteps.onFormPage().field(ADRES).input().sendKeys(ADDRESS);
        offerAddSteps.onFormPage().addressesSuggestList().get(FIRST).click();
        offerAddSteps.publish();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                age().withInputValue(AGE_VALUE),
                workExpirence().withSelectValue(WORK_EXPIRENCE_VALUE),
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE),
                sex().withSelectValue(SEX_VALUE),
                typeOfEmployment().withSelectValue(TYPE_OF_EMPLOYMENT_VALUE),
                education().withSelectValue(EDUCATION_VALUE));

        assertThat("Ответы должны совпадать", offerCard.getAttributes(), jsonEquals(attributes)
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории черновика с частичным сохранением атрибутов")
    public void shouldEditCategoryWithAttributesPartialySaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle()).withAttributes(
                        select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                        select(EDUCATION).setValue(EDUCATION_VALUE),
                        select(WORK_EXPIRENCE).setValue(WORK_EXPIRENCE_VALUE),
                        select(SEX).setValue(SEX_VALUE),
                        select(TYPE_OF_EMPLOYMENT).setValue(TYPE_OF_EMPLOYMENT_VALUE),
                        input(AGE).setValue(AGE_VALUE))
                .fillToAddressStep();

        saveDraftAndReturnToForm();

        offerAddSteps.onFormPage().field("Категория").link("Изменить").click();
        offerAddSteps.onFormPage().link(NO_SUITABLE).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Вакансии").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Продажи").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Менеджер").click();
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        offerAddSteps.onFormPage().field(ADRES).input().sendKeys(ADDRESS);
        offerAddSteps.onFormPage().addressesSuggestList().get(FIRST).click();
        offerAddSteps.publish();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                workExpirence().withSelectValue(WORK_EXPIRENCE_VALUE),
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE),
                typeOfEmployment().withSelectValue(TYPE_OF_EMPLOYMENT_VALUE));

        assertThat("Ответы должны совпадать", offerCard.getAttributes(), jsonEquals(attributes)
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    private void saveDraftAndReturnToForm() {
        offerAddSteps.onFormPage().back().click();
        offerAddSteps.onFormPage().modal().button("Сохранить черновик").waitUntil(isDisplayed()).click();
        urlSteps.open();
        offerAddSteps.onFormPage().button(CONTINUE).click();
    }

}
