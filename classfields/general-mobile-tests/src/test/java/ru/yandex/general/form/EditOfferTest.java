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
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.general.beans.Attribute.input;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.beans.card.CurrentPrice.currentPrice;
import static ru.yandex.general.consts.Attributes.typeOfEmployment;
import static ru.yandex.general.consts.Attributes.workExpirence;
import static ru.yandex.general.consts.Attributes.workSchedule;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Categories.USLUGI_DOSTAVKI;
import static ru.yandex.general.consts.FormConstants.Categories.VAKANCIYA_RABOCHII;
import static ru.yandex.general.consts.FormConstants.Conditions.NEW;
import static ru.yandex.general.consts.GeneralFeatures.EDIT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.page.FormPage.CENA;
import static ru.yandex.general.mobile.page.FormPage.CHANGE;
import static ru.yandex.general.mobile.page.FormPage.CHANGE_CATEGORY;
import static ru.yandex.general.mobile.page.FormPage.COMPLETE;
import static ru.yandex.general.mobile.page.FormPage.ADRES;
import static ru.yandex.general.mobile.page.FormPage.MOVE_TO_CATEGORY;
import static ru.yandex.general.mobile.page.FormPage.NAZVANIE;
import static ru.yandex.general.mobile.page.FormPage.OPISANIE;
import static ru.yandex.general.mobile.page.FormPage.SAVE;
import static ru.yandex.general.mobile.page.FormPage.SOSTOYANIE;
import static ru.yandex.general.mobile.page.FormPage.USED;
import static ru.yandex.general.mobile.page.FormPage.ZARPLATA;
import static ru.yandex.general.mobile.page.OfferCardPage.EDIT;
import static ru.yandex.general.page.FormPage.FIRST;
import static ru.yandex.general.page.FormPage.SEND_BY_TAXI;
import static ru.yandex.general.page.FormPage.SEND_RUSSIA;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(EDIT_FEATURE)
@DisplayName("Редактирование оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class EditOfferTest {

    private static final String NAME_EDITED = "Переноска для кота";
    private static final String DESCRIPTION = "Описание, несколько слов + символы";
    private static final String DESCRIPTION_EDITED = "Отредактированное описание";
    private static final String PRICE = "2499";
    private static final String PRICE_EDITED = "13200";
    private static final String SALLARY = "45000";
    private static final String SECOND_ADDRESS = "ул. Ленина, 12";
    private static final String CATEGORY = "Транспортировка, переноски";
    private static final String CATEGORY_EDITED = "Мобильные телефоны";
    private static final String PARENT_CATEGORY = "Животные и товары для них";
    private static final String MIDDLE_CATEGORY_EDITED = "Телефоны и умные часы";
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
        offerAddSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование оффера")
    public void shouldEditOffer() {
        offerAddSteps.withCategory(PERENOSKA)
                .withPrice(PRICE)
                .withCondition(NEW)
                .withDescription(DESCRIPTION)
                .addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().draftSection(NAZVANIE).input().clearInput().click();
        offerAddSteps.onFormPage().draftSection(NAZVANIE).input().sendKeys(NAME_EDITED);
        offerAddSteps.onFormPage().draftSection(OPISANIE).clearTextarea().click();
        offerAddSteps.onFormPage().draftSection(OPISANIE).textarea().sendKeys(DESCRIPTION_EDITED);
        offerAddSteps.onFormPage().draftSection(CENA).input().clearInput().click();
        offerAddSteps.onFormPage().draftSection(CENA).input().sendKeys(PRICE_EDITED);
        offerAddSteps.onFormPage().draftSection(SOSTOYANIE).spanLink(USED).click();
        offerAddSteps.onFormPage().draftSection(ADRES).input().click();
        offerAddSteps.onFormPage().wrapper("Первый адрес").input().click();
        offerAddSteps.onFormPage().wrapper(ADRES).clearTextarea().click();
        offerAddSteps.onFormPage().wrapper(ADRES).textarea().sendKeys(SECOND_ADDRESS);
        offerAddSteps.onFormPage().wrapper(ADRES).suggestItem(SECOND_ADDRESS).click();
        offerAddSteps.onFormPage().button(COMPLETE).click();
        offerAddSteps.onFormPage().checkboxWithLabel(SEND_BY_TAXI).click();
        offerAddSteps.onFormPage().checkboxWithLabel(SEND_RUSSIA).click();
        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE_EDITED);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(NAME_EDITED);
            s.assertThat(offerCard.getContacts().getAddresses().get(0).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(SECOND_ADDRESS);
            s.assertThat(offerCard.getDescription()).as("Описание").isEqualTo(DESCRIPTION_EDITED);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY);
            s.assertThat(offerCard.getCategory().getParents().get(0).getShortName()).as("Родительская категория")
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

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().link(PARENT_CATEGORY_EDITED).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().link(MIDDLE_CATEGORY_EDITED).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().link(CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории оффера через контрол категории")
    public void shouldEditCategoryOfferFromCategoryControl() {
        offerAddSteps.addOffer();
        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection("Категория").button().click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().link(PARENT_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().link(MIDDLE_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().link(CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Изменение цены на «Даром»")
    public void shouldEditPriceToFree() {
        offerAddSteps.withPrice(PRICE).addOffer();
        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection(CENA).switcher().click();
        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

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

        offerAddSteps.onFormPage().draftSection(CENA).input().clearInput().click();
        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        assertThatJson(offerCard.getPrice().getCurrentPrice().toString())
                .isEqualTo(currentPrice().setTypename("Unset").toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - товар на резюме")
    public void shouldEditCategoryFromProductToWork() {
        offerAddSteps.addOffer();
        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().link("Работа").click();
        offerAddSteps.onFormPage().popup().link("Резюме").click();
        offerAddSteps.onFormPage().popup().link("Продажи").click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(ZARPLATA).input().sendKeys(SALLARY);
        offerAddSteps.waitSomething(500, TimeUnit.MILLISECONDS);

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

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
        offerAddSteps.withCategory(REZUME_IN_SELLING).withPrice(SALLARY).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection("Категория").button().click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().backButton().click();
        offerAddSteps.onFormPage().popup().link(PARENT_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().link(MIDDLE_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().link(CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(SOSTOYANIE).spanLink(USED).click();
        offerAddSteps.onFormPage().draftSection(CENA).input().sendKeys(PRICE);
        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
        });
    }

    @Test
    @Issue("CLASSFRONT-1944")
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование категории - товар на услугу")
    public void shouldEditCategoryProductToUslugi() {
        offerAddSteps.addOffer();
        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().link("Услуги").click();
        offerAddSteps.onFormPage().popup().link("Предложения услуг").click();
        offerAddSteps.onFormPage().popup().link("Бытовые услуги").click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(CENA).input().sendKeys(PRICE);
        offerAddSteps.wait500MS();

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

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
    @DisplayName("Редактирование категории - резюме на услугу")
    public void shouldEditCategoryFromWorkToUslugi() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withPrice(SALLARY).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection("Категория").button().click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().backButton().click();
        offerAddSteps.onFormPage().popup().link("Услуги").click();
        offerAddSteps.onFormPage().popup().link("Предложения услуг").click();
        offerAddSteps.onFormPage().popup().link("Бытовые услуги").click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(CENA).input().sendKeys(PRICE);
        offerAddSteps.wait500MS();

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

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
        offerAddSteps.withCategory(USLUGI_DOSTAVKI).withPrice(PRICE).addOffer();
        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().button(CHANGE_CATEGORY).waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().backButton().click();
        offerAddSteps.onFormPage().popup().link("Работа").click();
        offerAddSteps.onFormPage().popup().link("Резюме").click();
        offerAddSteps.onFormPage().popup().link("Продажи").click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(ZARPLATA).input().sendKeys(SALLARY);
        offerAddSteps.wait500MS();

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

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
        offerAddSteps.withCategory(USLUGI_DOSTAVKI).withPrice(PRICE).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection("Категория").button().click();
        offerAddSteps.onFormPage().popup().backButton().waitUntil(isDisplayed()).click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().backButton().click();
        offerAddSteps.onFormPage().popup().link(PARENT_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().link(MIDDLE_CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().link(CATEGORY_EDITED).click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.onFormPage().draftSection(SOSTOYANIE).spanLink(USED).click();
        offerAddSteps.wait500MS();

        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo(CATEGORY_EDITED);
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(PARENT_CATEGORY_EDITED);
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование оффера, смена категории с сохранением атрибутов")
    public void shouldEditCategoryWithAttributesSaved() {
        offerAddSteps.withCategory(VAKANCIYA_RABOCHII).withAttributes(
                select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                select(WORK_EXPIRENCE).setValue(WORK_EXPIRENCE_VALUE),
                select(TYPE_OF_EMPLOYMENT).setValue(TYPE_OF_EMPLOYMENT_VALUE)).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection("Категория").button().click();
        offerAddSteps.onFormPage().popup().link("Агрономы").waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE),
                workExpirence().withSelectValue(WORK_EXPIRENCE_VALUE),
                typeOfEmployment().withSelectValue(TYPE_OF_EMPLOYMENT_VALUE));

        assertThat("Ответы должны совпадать", offerCard.getAttributes(), jsonEquals(attributes)
                .when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактирование оффера, смена категории с частичным сохранением атрибутов")
    public void shouldEditCategoryWithAttributesPartialySaved() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).withName(REZUME_IN_SELLING.getTitle()).withAttributes(
                select(WORK_SCHEDULE).setValue(WORK_SCHEDULE_VALUE),
                select(EDUCATION).setValue(EDUCATION_VALUE),
                select(SEX).setValue(SEX_VALUE),
                input(AGE).setValue(AGE_VALUE)).addOffer();

        offerAddSteps.onOfferCardPage().link(EDIT).waitUntil(isDisplayed()).click();

        offerAddSteps.onFormPage().draftSection("Категория").button().click();
        offerAddSteps.onFormPage().popup().backButton().click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().popup().link("Вакансии").waitUntil(isDisplayed()).click();
        offerAddSteps.onFormPage().popup().link("Продажи").click();
        offerAddSteps.onFormPage().popup().link("Менеджеры").click();
        offerAddSteps.onFormPage().popup().button(MOVE_TO_CATEGORY).click();
        offerAddSteps.onFormPage().button(CHANGE).click();
        offerAddSteps.finalStep(SAVE);
        offerAddSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                workSchedule().withSelectValue(WORK_SCHEDULE_VALUE));

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
        offerAddSteps.onFormPage().checkboxWithLabel(SEND_BY_TAXI).click();
        offerAddSteps.onFormPage().checkboxWithLabel(SEND_RUSSIA).click();
        offerAddSteps.finalStep(SAVE);

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
        offerAddSteps.onFormPage().checkboxWithLabel(SEND_BY_TAXI).click();
        offerAddSteps.onFormPage().checkboxWithLabel(SEND_RUSSIA).click();
        offerAddSteps.finalStep(SAVE);

        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(true);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(false);
        });
    }

}
