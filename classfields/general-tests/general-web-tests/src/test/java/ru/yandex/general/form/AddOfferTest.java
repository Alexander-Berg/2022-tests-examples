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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.Attribute.input;
import static ru.yandex.general.beans.Attribute.multiselect;
import static ru.yandex.general.beans.Attribute.select;
import static ru.yandex.general.beans.Attribute.switcher;
import static ru.yandex.general.beans.card.CurrentPrice.currentPrice;
import static ru.yandex.general.consts.Attributes.bluetooth;
import static ru.yandex.general.consts.Attributes.driverLicenseCategory;
import static ru.yandex.general.consts.Attributes.manufacturer;
import static ru.yandex.general.consts.Attributes.powerSupplyType;
import static ru.yandex.general.consts.Attributes.voiceAssistant;
import static ru.yandex.general.consts.Attributes.workTime;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.FormConstants.Categories.USLUGI_DOSTAVKI;
import static ru.yandex.general.consts.FormConstants.Categories.VAKANCIYA_MENEGER;
import static ru.yandex.general.consts.FormConstants.Conditions.NEW;
import static ru.yandex.general.consts.FormConstants.Conditions.USED;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.page.FormPage.FIRST;
import static ru.yandex.general.page.FormPage.SECOND;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@DisplayName("Размещение оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AddOfferTest {

    private static final String DESCRIPTION = "Описание, несколько слов + символы";
    private static final String PRICE = "2499";
    private static final String SALLARY = "45000";
    private static final String ADDRESS = "проспект Карла Маркса, 2";
    private static final String SECOND_ADDRESS = "ул. Ленина, 12";
    private static final String WORK_TIME = "Время работы, час";
    private static final double SUMMARY_POWER_VALUE = 125.0;
    private static final String BLUETOOTH = "Bluetooth";
    private static final String TIP_PITANIYA = "Тип питания";
    private static final String APPLE = "Apple";
    private static final String APPLE_SIRI = "Apple Siri";
    private static final String FROM_AKKUM = "от аккумулятора";
    private static final String FROM_NETWORK = "от сети";

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
    @DisplayName("Размещение оффера")
    public void shouldAddOffer() {
        category = PERENOSKA;

        offerAddSteps.withCategory(category)
                .withName(category.getTitle())
                .withDescription(DESCRIPTION)
                .withPrice(PRICE)
                .withAddress(ADDRESS)
                .withCondition(NEW)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
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
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(false);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(false);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера «Даром»")
    public void shouldAddOfferWithFreePrice() {
        offerAddSteps.withCondition(USED).withFreePrice().addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getTypename()).as("Цена").isEqualTo("Free");
            s.assertThat(offerCard.getCondition()).as("Состояние").isEqualTo(USED.getValue());
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера без указания цены")
    public void shouldAddOfferWithNoPrice() {
        offerAddSteps.addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        assertThatJson(offerCard.getPrice().getCurrentPrice().toString())
                .isEqualTo(currentPrice().setTypename("Unset").toString());

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера с двумя адресами")
    public void shouldAddOffer2Address() {
        offerAddSteps.withAddress(ADDRESS, SECOND_ADDRESS).addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress())
                    .as("Первый адрес").isEqualTo(ADDRESS);
            s.assertThat(offerCard.getContacts().getAddresses().get(SECOND).getAddress().getAddress())
                    .as("Второй адрес").isEqualTo(SECOND_ADDRESS);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера со всеми типами атрибутов")
    public void shouldAddOfferWithAttributes() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).withAttributes(
                select("Производитель").setValue(APPLE),
                select("Голосовой помощник").setValue("Apple Siri"),
                multiselect(TIP_PITANIYA).setValues(FROM_AKKUM, FROM_NETWORK),
                input(WORK_TIME).setValue("35"),
                switcher(BLUETOOTH)).addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(
                workTime().withInputValue("35"),
                bluetooth().withBooleanValue(true),
                powerSupplyType().withMultiselectValue(FROM_AKKUM, FROM_NETWORK),
                manufacturer().withSelectValue(APPLE),
                voiceAssistant().withSelectValue(APPLE_SIRI));

        assertThatJson(offerCard.getAttributes().toString()).when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(attributes.toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера, в категории «Резюме»")
    public void shouldAddRezumeOffer() {
        category = REZUME_IN_SELLING;

        offerAddSteps.withCategory(category)
                .withName(category.getTitle())
                .withDescription(DESCRIPTION)
                .withPrice(SALLARY)
                .withAddress(ADDRESS)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getSalaryRur()).as("Зарплата").isEqualTo(SALLARY);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(category.getTitle());
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(ADDRESS);
            s.assertThat(offerCard.getDescription()).as("Описание").isEqualTo(DESCRIPTION);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Продажи");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(category.getParentCategory());
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера, в категории «Вакансии»")
    public void shouldAddVakansiiOffer() {
        category = VAKANCIYA_MENEGER;

        offerAddSteps.withCategory(category)
                .withName(category.getTitle())
                .withDescription(DESCRIPTION)
                .withPrice(SALLARY)
                .withAddress(ADDRESS)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getSalaryRur()).as("Зарплата").isEqualTo(SALLARY);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(category.getTitle());
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(ADDRESS);
            s.assertThat(offerCard.getDescription()).as("Описание").isEqualTo(DESCRIPTION);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория").isEqualTo("Менеджеры");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(category.getParentCategory());
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера, в категории «Резюме», без указания зарплаты")
    public void shouldAddRezumeOfferWithoutSallary() {
        category = REZUME_IN_SELLING;

        offerAddSteps.withCategory(category)
                .withDescription(DESCRIPTION)
                .withAddress(ADDRESS)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        assertThatJson(offerCard.getPrice().getCurrentPrice().toString())
                .isEqualTo(currentPrice().setTypename("Unset").toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера с доставкой курьером")
    public void shouldAddOfferCourierDelivery() {
        category = PERENOSKA;

        offerAddSteps.withCategory(category)
                .withSendByCourier(true)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());
        System.out.println(offerCard.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(true);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(false);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера с доставкой по России")
    public void shouldAddOfferRussiaDelivery() {
        category = PERENOSKA;

        offerAddSteps.withCategory(category)
                .withSendWithinRussia(true)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());
        System.out.println(offerCard.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(false);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(true);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера с доставкой курьером и по России")
    public void shouldAddOfferCourierAndRussiaDelivery() {
        category = PERENOSKA;

        offerAddSteps.withCategory(category)
                .withSendByCourier(true)
                .withSendWithinRussia(true)
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());
        System.out.println(offerCard.toString());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendByCourier()).as("Доставка курьером")
                    .isEqualTo(true);
            s.assertThat(offerCard.getDeliveryInfo().getSelfDelivery().isSendWithinRussia()).as("Доставка по России")
                    .isEqualTo(true);
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера, в категории «Услуги»")
    public void shouldAddUslugiOffer() {
        category = USLUGI_DOSTAVKI;
        offerAddSteps.withCategory(category)
                .withPrice(PRICE)
                .withAttributes(multiselect("Категория прав").setValues("A", "B"))
                .withName(category.getTitle())
                .addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        List<Attribute> attributes = Arrays.asList(driverLicenseCategory().withMultiselectValue("A", "B"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getPrice().getCurrentPrice().getPriceRur()).as("Цена").isEqualTo(PRICE);
            s.assertThat(offerCard.getTitle()).as("Название").isEqualTo(category.getTitle());
            s.assertThat(offerCard.getContacts().getAddresses().get(FIRST).getAddress().getAddress()).as("Адрес")
                    .isEqualTo(ADDRESS);
            s.assertThat(offerCard.getCategory().getShortName()).as("Категория")
                    .isEqualTo("Курьеры и грузоперевозки");
            s.assertThat(offerCard.getCategory().getParents().get(FIRST).getShortName()).as("Родительская категория")
                    .isEqualTo(category.getParentCategory());
            s.assertThat(offerCard.getAttributes().toString()).as("Атрибуты").isEqualTo(attributes.toString());
        });
    }

}
