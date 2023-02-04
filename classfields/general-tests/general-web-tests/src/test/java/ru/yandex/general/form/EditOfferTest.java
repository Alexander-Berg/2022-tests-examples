package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Arrays;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.general.beans.Attribute.input;
import static ru.yandex.general.beans.Attribute.multiselect;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.beans.Attribute.switcher;
import static ru.yandex.general.beans.card.CurrentPrice.currentPrice;
import static ru.yandex.general.consts.Attributes.age;
import static ru.yandex.general.consts.Attributes.bluetooth;
import static ru.yandex.general.consts.Attributes.education;
import static ru.yandex.general.consts.Attributes.sex;
import static ru.yandex.general.consts.Attributes.typeOfEmployment;
import static ru.yandex.general.consts.Attributes.workExpirence;
import static ru.yandex.general.consts.Attributes.workSchedule;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.FormConstants.Categories.USLUGI_DOSTAVKI;
import static ru.yandex.general.consts.GeneralFeatures.EDIT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.ADD_MORE_ADDRESS;
import static ru.yandex.general.page.FormPage.ADRES;
import static ru.yandex.general.page.FormPage.CHANGE_CATEGORY;
import static ru.yandex.general.page.FormPage.FIRST;
import static ru.yandex.general.page.FormPage.GIVE_FREE;
import static ru.yandex.general.page.FormPage.NAZVANIE;
import static ru.yandex.general.page.FormPage.NEW_PRODUCT;
import static ru.yandex.general.page.FormPage.OPISANIE;
import static ru.yandex.general.page.FormPage.SEND_BY_TAXI;
import static ru.yandex.general.page.FormPage.SEND_RUSSIA;
import static ru.yandex.general.page.FormPage.USED;
import static ru.yandex.general.page.FormPage.ZARPLATA;
import static ru.yandex.general.page.OfferCardPage.EDIT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(EDIT_FEATURE)
@DisplayName("Редактирование оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class EditOfferTest {

    private static final String NAME_EDITED = "Переноска для кота";
    private static final String DESCRIPTION = "Описание, несколько слов + символы";
    private static final String DESCRIPTION_EDITED = "Отредактированное описание";
    private static final String PRICE = "2499";
    private static final String PRICE_EDITED = "13200";
    private static final String SALLARY = "45000";
    private static final String ADDRESS = "проспект Карла Маркса, 2";
    private static final String SECOND_ADDRESS = "ул. Ленина, 12";
    private static final String CATEGORY = "Транспортировка, переноски";
    private static final String CATEGORY_EDITED = "Мобильные телефоны";
    private static final String CATEGORY_BREADCRUMBS = "Миски, кормушки и поилки";
    private static final String PARENT_CATEGORY = "Животные и товары для них";
    private static final String PARENT_CATEGORY_EDITED = "Электроника";
    private static final String WORK_TIME = "Время работы, час";
    private static final String BLUETOOTH = "Bluetooth";
    private static final String TIP_PITANIYA = "Тип питания";
    private static final String GOLOSOVOI_POMOSCHNIK = "Голосовой помощник";
    private static final String MANUFACTURER = "Производитель";
    private static final String APPLE = "Apple";
    private static final String FROM_AKKUM = "от аккумулятора";
    private static final String FROM_NETWORK = "от сети";
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
    @DisplayName("Редактирование полей оффера")
    public void shouldEditOffer() {
        offerAddSteps.withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .withAddress(ADDRESS)
                .addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().field(NAZVANIE).input().clearInput().click();
        offerAddSteps.onFormPage().field(NAZVANIE).input().sendKeys(NAME_EDITED);
        offerAddSteps.onFormPage().field(OPISANIE).clearTextarea().click();
        offerAddSteps.onFormPage().field(OPISANIE).textarea().sendKeys(DESCRIPTION_EDITED);
        offerAddSteps.onFormPage().price().clearInput().click();
        offerAddSteps.onFormPage().price().sendKeys(PRICE_EDITED);
        offerAddSteps.onFormPage().condition(USED).click();
        offerAddSteps.onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        offerAddSteps.onFormPage().field(ADRES).input().clearButton().click();
        offerAddSteps.onFormPage().field(ADRES).input().sendKeys(SECOND_ADDRESS);
        offerAddSteps.onFormPage().addressesSuggestList().get(FIRST).click();
        offerAddSteps.onFormPage().delivery().checkboxWithLabel(SEND_BY_TAXI).click();
        offerAddSteps.onFormPage().delivery().checkboxWithLabel(SEND_RUSSIA).click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE_EDITED);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(NAME_EDITED);
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo("Used");
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(SECOND_ADDRESS);
            s.assertThat(offerCard.getDescription()).as("Описание").isEqualTo(DESCRIPTION_EDITED);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo((CATEGORY));
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(true);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(true);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории оффера через кнопку «Сменить категорию»")
    public void shouldEditCategoryOfferFromChangeCategoryButton() {
        offerAddSteps.addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link(PARENT_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().modal().link("Телефоны и умные часы").click();
        offerAddSteps.onFormPage().modal().link(CATEGORY_EDITED).click();

        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории оффера через хлебные крошки")
    public void shouldEditCategoryOfferFromBreadcrumbs() {
        offerAddSteps.addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().breadcrumbs().link(PARENT_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link(CATEGORY_BREADCRUMBS).click();

        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_BREADCRUMBS);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очистка атрибутов оффера при редактировании")
    public void shouldClearOfferAttributes() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).withAttributes(
                select("Производитель").setValue(APPLE),
                select("Голосовой помощник").setValue("Apple Siri"),
                multiselect(TIP_PITANIYA).setValues(FROM_AKKUM, FROM_NETWORK),
                input(WORK_TIME).setValue("55"),
                switcher(BLUETOOTH)).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().attribute(GOLOSOVOI_POMOSCHNIK).input().clearInput().click();
        offerAddSteps.onFormPage().attribute(GOLOSOVOI_POMOSCHNIK).click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).input().clearInput().click();
        offerAddSteps.onFormPage().attribute(MANUFACTURER).click();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).input().clearInput().click();
        offerAddSteps.onFormPage().attribute(TIP_PITANIYA).click();
        offerAddSteps.onFormPage().attribute(WORK_TIME).input().clearInput().click();
        offerAddSteps.onFormPage().attribute(WORK_TIME).click();
        offerAddSteps.onFormPage().attribute(BLUETOOTH).switcher().click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                bluetooth().withBooleanValue(false));

        assertThatJson(offerCard.getAttributes().toString()).isEqualTo(attributes.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Изменение цены на «Даром»")
    public void shouldEditPriceToFree() {
        offerAddSteps.withPrice(PRICE).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().checkboxWithLabel(GIVE_FREE).click();
        offerAddSteps.save();
        offerAddSteps.wait500MS();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getTypename()).as("Цена").isEqualTo("Free");
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Изменение цены на неуказанную")
    public void shouldEditPriceToUnset() {
        offerAddSteps.withPrice(PRICE).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().price().clearInput().click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        assertThatJson(offerCard.getPrice().getCurrentPrice().toString())
                .isEqualTo(currentPrice().setTypename("Unset").toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - товар на резюме")
    public void shouldEditCategoryFromProductToWork() {
        offerAddSteps.withCategory(PERENOSKA).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Резюме").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Продажи").click();
        offerAddSteps.onFormPage().field(ZARPLATA).input().sendKeys(SALLARY);
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getSalaryRur()).as("Зарплата").isEqualTo(SALLARY);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Продажи");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo("Работа");
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - резюме на товар")
    public void shouldEditCategoryFromWorkToProduct() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle()).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link(PARENT_CATEGORY_EDITED).waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Телефоны и умные часы").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link(CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().condition(NEW_PRODUCT).click();
        offerAddSteps.onFormPage().price().sendKeys(PRICE);
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo("New");
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - товар на услугу")
    public void shouldEditCategoryFromProductToUslugi() {
        offerAddSteps.withCategory(PERENOSKA).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link("Услуги").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Предложения услуг").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Бытовые услуги").click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Бытовые услуги");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo("Услуги");
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(null);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - резюме на услугу")
    public void shouldEditCategoryFromWorkToUslugi() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle()).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link("Услуги").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Предложения услуг").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Бытовые услуги").click();
        offerAddSteps.onFormPage().price().sendKeys(PRICE);
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Бытовые услуги");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo("Услуги");
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - услугу на резюме")
    public void shouldEditCategoryUslugiToWork() {
        offerAddSteps.withCategory(USLUGI_DOSTAVKI).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Резюме").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Продажи").click();
        offerAddSteps.onFormPage().field(ZARPLATA).input().sendKeys(SALLARY);
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getSalaryRur()).as("Зарплата").isEqualTo(SALLARY);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Продажи");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo("Работа");
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - услугу на товар")
    public void shouldEditCategoryUslugiToProduct() {
        offerAddSteps.withCategory(USLUGI_DOSTAVKI).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.onFormPage().modal().link(PARENT_CATEGORY_EDITED).waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Телефоны и умные часы").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link(CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().condition(NEW_PRODUCT).click();
        offerAddSteps.onFormPage().price().sendKeys(PRICE);
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo("New");
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование оффера, смена категории с сохранением атрибутов")
    public void shouldEditCategoryWithAttributesSaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle()).withAttributes(
                select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                select(EDUCATION).setValue(EDUCATION_VALUE),
                select(WORK_EXPIRENCE).setValue(WORK_EXPIRENCE_VALUE),
                select(SEX).setValue(SEX_VALUE),
                select(TYPE_OF_EMPLOYMENT).setValue(TYPE_OF_EMPLOYMENT_VALUE),
                input(AGE).setValue(AGE_VALUE)).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Резюме").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Продажи").click();
        offerAddSteps.save();

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
    @DisplayName("Редактирование оффера, смена категории с частичным сохранением атрибутов")
    public void shouldEditCategoryWithAttributesPartialySaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle())
                .withAttributes(
                        select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                        select(EDUCATION).setValue(EDUCATION_VALUE),
                        select(WORK_EXPIRENCE).setValue(WORK_EXPIRENCE_VALUE),
                        select(SEX).setValue(SEX_VALUE),
                        select(TYPE_OF_EMPLOYMENT).setValue(TYPE_OF_EMPLOYMENT_VALUE),
                        input(AGE).setValue(AGE_VALUE)).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Работа").waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Вакансии").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Продажи").click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().modal().link("Менеджеры").click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                workExpirence().withSelectValue(WORK_EXPIRENCE_VALUE),
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE),
                typeOfEmployment().withSelectValue(TYPE_OF_EMPLOYMENT_VALUE));

        assertThat("Ответы должны совпадать", offerCard.getAttributes(), jsonEquals(attributes)
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование оффера, убираем доставку")
    public void shouldRemoveOfferDelivery() {
        offerAddSteps.withSendByCourier(true).withSendWithinRussia(true)
                .addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().delivery().checkboxWithLabel(SEND_BY_TAXI).click();
        offerAddSteps.onFormPage().delivery().checkboxWithLabel(SEND_RUSSIA).click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(false);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(false);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование оффера, меняем доставку по России на доставку курьером")
    public void shouldChangeRussiaDeliveryToCourier() {
        offerAddSteps.withSendByCourier(false).withSendWithinRussia(true)
                .addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().delivery().checkboxWithLabel(SEND_BY_TAXI).click();
        offerAddSteps.onFormPage().delivery().checkboxWithLabel(SEND_RUSSIA).click();
        offerAddSteps.save();

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(true);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(false);
        });
    }

}
