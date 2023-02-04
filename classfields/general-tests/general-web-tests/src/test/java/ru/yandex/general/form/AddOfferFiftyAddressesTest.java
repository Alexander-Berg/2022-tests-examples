package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.beans.card.Card;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GraphqlSteps;
import ru.yandex.general.step.OfferAddSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@DisplayName("Размещение оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AddOfferFiftyAddressesTest {

    private static final String ADDRESS_1 = "проспект Карла Маркса, 2";
    private static final String ADDRESS_2 = "ул. Ленина, 12";
    private static final String ADDRESS_3 = "Павелецкая площадь, 2с1";
    private static final String ADDRESS_4 = "Павелецкая площадь, 2с2";
    private static final String ADDRESS_5 = "Павелецкая площадь, 2с3";
    private static final String ADDRESS_6 = "Павелецкая площадь, 2с4";
    private static final String ADDRESS_7 = "Павелецкая площадь, 1";
    private static final String ADDRESS_8 = "Павелецкая площадь, 1Ас1";
    private static final String ADDRESS_9 = "Павелецкая площадь, 1Ас2";
    private static final String ADDRESS_10 = "Замоскворечье";
    private static final String ADDRESS_11 = "Соколиная гора";
    private static final String ADDRESS_12 = "Невский проспект, 88";
    private static final String ADDRESS_13 = "Невский проспект, 88Б";
    private static final String ADDRESS_14 = "Невский проспект, 88Д";
    private static final String ADDRESS_15 = "Невский проспект, 88Ю";
    private static final String ADDRESS_16 = "Гостиный двор";
    private static final String ADDRESS_17 = "ул. Новый Арбат, 21";
    private static final String ADDRESS_18 = "ул. Новый Арбат, 21с1";
    private static final String ADDRESS_19 = "ул. Новый Арбат, 21с2";
    private static final String ADDRESS_20 = "ул. Новый Арбат, 20";
    private static final String ADDRESS_21 = "Маршала Покрышкина";
    private static final String ADDRESS_22 = "Заельцовский район";
    private static final String ADDRESS_23 = "ул. Новый Арбат, 1с2";
    private static final String ADDRESS_24 = "ул. Новый Арбат, 1с3";
    private static final String ADDRESS_25 = "ул. Новый Арбат, 1с4";
    private static final String ADDRESS_26 = "ул. Новый Арбат, 1с5";
    private static final String ADDRESS_27 = "ул. Охотный Ряд, 1";
    private static final String ADDRESS_28 = "ул. Охотный Ряд, 1с2";
    private static final String ADDRESS_29 = "ул. Охотный Ряд, 1к2с2";
    private static final String ADDRESS_30 = "Дубнинская улица, 4к1";
    private static final String ADDRESS_31 = "Дубнинская улица, 5к1";
    private static final String ADDRESS_32 = "Дмитровское шоссе, 1А";
    private static final String ADDRESS_33 = "Дмитровское шоссе, 1Б";
    private static final String ADDRESS_34 = "Дмитровское шоссе, 1В";
    private static final String ADDRESS_35 = "Дмитровское шоссе, 2";
    private static final String ADDRESS_36 = "Дмитровское шоссе, 2с3";
    private static final String ADDRESS_37 = "Дмитровское шоссе, 2с4";
    private static final String ADDRESS_38 = "Дмитровское шоссе, 2с5";
    private static final String ADDRESS_39 = "Дмитровское шоссе, 3к1";
    private static final String ADDRESS_40 = "Дмитровское шоссе, 40к1";
    private static final String ADDRESS_41 = "Дмитровское шоссе, 41к1";
    private static final String ADDRESS_42 = "Дмитровское шоссе, 41к1с2";
    private static final String ADDRESS_43 = "Дмитровское шоссе, 5Ас1";
    private static final String ADDRESS_44 = "Дмитровское шоссе, 5Ас2";
    private static final String ADDRESS_45 = "Дмитровское шоссе, 5Ас3";
    private static final String ADDRESS_46 = "Дмитровское шоссе, 60А";
    private static final String ADDRESS_47 = "Дмитровское шоссе, 60Ас1";
    private static final String ADDRESS_48 = "Дмитровское шоссе, 60Бс1";
    private static final String ADDRESS_49 = "Дмитровское шоссе, 7";
    private static final String ADDRESS_50 = "Дмитровское шоссе, 7к1";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

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
        offerAddSteps.setMoscowCookie();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Размещение оффера с 50 адресами, разные типы адреса в разных городах")
    public void shouldAddOffer50Address() {
        offerAddSteps.withAddress(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, ADDRESS_5, ADDRESS_6, ADDRESS_7,
                ADDRESS_8, ADDRESS_9, ADDRESS_10, ADDRESS_11, ADDRESS_12, ADDRESS_13, ADDRESS_14, ADDRESS_15,
                ADDRESS_16, ADDRESS_17, ADDRESS_18, ADDRESS_19, ADDRESS_20, ADDRESS_21, ADDRESS_22, ADDRESS_23,
                ADDRESS_24, ADDRESS_25, ADDRESS_26, ADDRESS_27, ADDRESS_28, ADDRESS_29, ADDRESS_30, ADDRESS_31,
                ADDRESS_32, ADDRESS_33, ADDRESS_34, ADDRESS_35, ADDRESS_36, ADDRESS_37, ADDRESS_38, ADDRESS_39,
                ADDRESS_40, ADDRESS_41, ADDRESS_42, ADDRESS_43, ADDRESS_44, ADDRESS_45, ADDRESS_46, ADDRESS_47,
                ADDRESS_48, ADDRESS_49, ADDRESS_50).addOffer();

        offerAddSteps.onOfferCardPage().title().waitUntil(isDisplayed());
        offerAddSteps.waitSomething(5, TimeUnit.SECONDS);
        Card offerCard = graphqlSteps.getOfferCard(urlSteps.getOfferId());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(offerCard.getContacts().getAddresses().get(0).getAddress().getAddress())
                    .as("1 адрес").isEqualTo(ADDRESS_1);
            s.assertThat(offerCard.getContacts().getAddresses().get(1).getAddress().getAddress())
                    .as("2 адрес").isEqualTo(ADDRESS_2);
            s.assertThat(offerCard.getContacts().getAddresses().get(2).getAddress().getAddress())
                    .as("3 адрес").isEqualTo(ADDRESS_3);
            s.assertThat(offerCard.getContacts().getAddresses().get(3).getAddress().getAddress())
                    .as("4 адрес").isEqualTo(ADDRESS_4);
            s.assertThat(offerCard.getContacts().getAddresses().get(4).getAddress().getAddress())
                    .as("5 адрес").isEqualTo(ADDRESS_5);
            s.assertThat(offerCard.getContacts().getAddresses().get(5).getAddress().getAddress())
                    .as("6 адрес").isEqualTo(ADDRESS_6);
            s.assertThat(offerCard.getContacts().getAddresses().get(6).getAddress().getAddress())
                    .as("7 адрес").isEqualTo(ADDRESS_7);
            s.assertThat(offerCard.getContacts().getAddresses().get(7).getAddress().getAddress())
                    .as("8 адрес").isEqualTo(ADDRESS_8);
            s.assertThat(offerCard.getContacts().getAddresses().get(8).getAddress().getAddress())
                    .as("9 адрес").isEqualTo(ADDRESS_9);
            s.assertThat(offerCard.getContacts().getAddresses().get(9).getDistrict().getName())
                    .as("10 адрес").isEqualTo(ADDRESS_10);
            s.assertThat(offerCard.getContacts().getAddresses().get(10).getMetroStation().getName())
                    .as("11 адрес").isEqualTo(ADDRESS_11);
            s.assertThat(offerCard.getContacts().getAddresses().get(11).getAddress().getAddress())
                    .as("12 адрес").isEqualTo(ADDRESS_12);
            s.assertThat(offerCard.getContacts().getAddresses().get(12).getAddress().getAddress())
                    .as("13 адрес").isEqualTo(ADDRESS_13);
            s.assertThat(offerCard.getContacts().getAddresses().get(13).getAddress().getAddress())
                    .as("14 адрес").isEqualTo(ADDRESS_14);
            s.assertThat(offerCard.getContacts().getAddresses().get(14).getAddress().getAddress())
                    .as("15 адрес").isEqualTo(ADDRESS_15);
            s.assertThat(offerCard.getContacts().getAddresses().get(15).getMetroStation().getName())
                    .as("16 адрес").isEqualTo(ADDRESS_16);
            s.assertThat(offerCard.getContacts().getAddresses().get(16).getAddress().getAddress())
                    .as("17 адрес").isEqualTo(ADDRESS_17);
            s.assertThat(offerCard.getContacts().getAddresses().get(17).getAddress().getAddress())
                    .as("18 адрес").isEqualTo(ADDRESS_18);
            s.assertThat(offerCard.getContacts().getAddresses().get(18).getAddress().getAddress())
                    .as("19 адрес").isEqualTo(ADDRESS_19);
            s.assertThat(offerCard.getContacts().getAddresses().get(19).getAddress().getAddress())
                    .as("20 адрес").isEqualTo(ADDRESS_20);
            s.assertThat(offerCard.getContacts().getAddresses().get(20).getMetroStation().getName())
                    .as("21 адрес").isEqualTo(ADDRESS_21);
            s.assertThat(offerCard.getContacts().getAddresses().get(21).getDistrict().getName())
                    .as("22 адрес").isEqualTo(ADDRESS_22);
            s.assertThat(offerCard.getContacts().getAddresses().get(22).getAddress().getAddress())
                    .as("23 адрес").isEqualTo(ADDRESS_23);
            s.assertThat(offerCard.getContacts().getAddresses().get(23).getAddress().getAddress())
                    .as("24 адрес").isEqualTo(ADDRESS_24);
            s.assertThat(offerCard.getContacts().getAddresses().get(24).getAddress().getAddress())
                    .as("25 адрес").isEqualTo(ADDRESS_25);
            s.assertThat(offerCard.getContacts().getAddresses().get(25).getAddress().getAddress())
                    .as("26 адрес").isEqualTo(ADDRESS_26);
            s.assertThat(offerCard.getContacts().getAddresses().get(26).getAddress().getAddress())
                    .as("27 адрес").isEqualTo(ADDRESS_27);
            s.assertThat(offerCard.getContacts().getAddresses().get(27).getAddress().getAddress())
                    .as("28 адрес").isEqualTo(ADDRESS_28);
            s.assertThat(offerCard.getContacts().getAddresses().get(28).getAddress().getAddress())
                    .as("29 адрес").isEqualTo(ADDRESS_29);
            s.assertThat(offerCard.getContacts().getAddresses().get(29).getAddress().getAddress())
                    .as("30 адрес").isEqualTo(ADDRESS_30);
            s.assertThat(offerCard.getContacts().getAddresses().get(30).getAddress().getAddress())
                    .as("31 адрес").isEqualTo(ADDRESS_31);
            s.assertThat(offerCard.getContacts().getAddresses().get(31).getAddress().getAddress())
                    .as("32 адрес").isEqualTo(ADDRESS_32);
            s.assertThat(offerCard.getContacts().getAddresses().get(32).getAddress().getAddress())
                    .as("33 адрес").isEqualTo(ADDRESS_33);
            s.assertThat(offerCard.getContacts().getAddresses().get(33).getAddress().getAddress())
                    .as("34 адрес").isEqualTo(ADDRESS_34);
            s.assertThat(offerCard.getContacts().getAddresses().get(34).getAddress().getAddress())
                    .as("35 адрес").isEqualTo(ADDRESS_35);
            s.assertThat(offerCard.getContacts().getAddresses().get(35).getAddress().getAddress())
                    .as("36 адрес").isEqualTo(ADDRESS_36);
            s.assertThat(offerCard.getContacts().getAddresses().get(36).getAddress().getAddress())
                    .as("37 адрес").isEqualTo(ADDRESS_37);
            s.assertThat(offerCard.getContacts().getAddresses().get(37).getAddress().getAddress())
                    .as("38 адрес").isEqualTo(ADDRESS_38);
            s.assertThat(offerCard.getContacts().getAddresses().get(38).getAddress().getAddress())
                    .as("39 адрес").isEqualTo(ADDRESS_39);
            s.assertThat(offerCard.getContacts().getAddresses().get(39).getAddress().getAddress())
                    .as("40 адрес").isEqualTo(ADDRESS_40);
            s.assertThat(offerCard.getContacts().getAddresses().get(40).getAddress().getAddress())
                    .as("41 адрес").isEqualTo(ADDRESS_41);
            s.assertThat(offerCard.getContacts().getAddresses().get(41).getAddress().getAddress())
                    .as("42 адрес").isEqualTo(ADDRESS_42);
            s.assertThat(offerCard.getContacts().getAddresses().get(42).getAddress().getAddress())
                    .as("43 адрес").isEqualTo(ADDRESS_43);
            s.assertThat(offerCard.getContacts().getAddresses().get(43).getAddress().getAddress())
                    .as("44 адрес").isEqualTo(ADDRESS_44);
            s.assertThat(offerCard.getContacts().getAddresses().get(44).getAddress().getAddress())
                    .as("45 адрес").isEqualTo(ADDRESS_45);
            s.assertThat(offerCard.getContacts().getAddresses().get(45).getAddress().getAddress())
                    .as("46 адрес").isEqualTo(ADDRESS_46);
            s.assertThat(offerCard.getContacts().getAddresses().get(46).getAddress().getAddress())
                    .as("47 адрес").isEqualTo(ADDRESS_47);
            s.assertThat(offerCard.getContacts().getAddresses().get(47).getAddress().getAddress())
                    .as("48 адрес").isEqualTo(ADDRESS_48);
            s.assertThat(offerCard.getContacts().getAddresses().get(48).getAddress().getAddress())
                    .as("49 адрес").isEqualTo(ADDRESS_49);
            s.assertThat(offerCard.getContacts().getAddresses().get(49).getAddress().getAddress())
                    .as("50 адрес").isEqualTo(ADDRESS_50);
        });
    }

}
