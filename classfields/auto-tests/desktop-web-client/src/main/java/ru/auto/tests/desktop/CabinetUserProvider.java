package ru.auto.tests.desktop;

import ru.auto.tests.desktop.utils.Utils;
import ru.auto.tests.passport.account.Account;

import javax.inject.Provider;
import java.util.Optional;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 29.03.18
 */
public class CabinetUserProvider {
    //Москва Прямой
    public static final Provider<Account> CABINET_USER = () -> Account.builder()
            .id("14090654")
            .login("aristos@ma.ru")
            .password("autoru").build();

    public static final Provider<Account> CABINET_USER_2 = () -> Account.builder()
            .id("23117336")
            .login("test.autoru@yandex.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> CABINET_USER_PAYMENT = () -> Account.builder()
            .id("11296277")
            .login("demo@auto.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> CABINET_USER_OVERDRAFT = () -> Account.builder()
            .id("29543726") //32452
            .login("vikupam@yandex.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    /* Следующих дилеров использовать только для тестов калькулятора */

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_MOSCOW = () -> Account.builder()
            .id("10843244") //23310
            .login("5188880@mail.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_MOSCOW_2 = () -> Account.builder()
            .id("30679") //208
            .login("info@a-motion.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_EKB_2 = () -> Account.builder()
            .id("24151684") //21151
            .login("om-metallurgov@e1.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_1_6_CITIES_2 = () -> Account.builder()
            .id("2032087") //1034
            .login("trade-in@landrover.vrn.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_7_CITIES_2 = () -> Account.builder()
            .id("11618471") //16541
            .login("mstartsev@mercedes-irk.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_MOSCOW_2 = () -> Account.builder()
            .id("3849112") //2222
            .login("used@genser.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_EKB_2 = () -> Account.builder()
            .id("28296254") //31464
            .login("biz.avtoban@mail.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_1_6_CITIES_2 = () -> Account.builder()
            .id("26034204") //8957
            .login("used171@avtomir.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_7_CITIES_2 = () -> Account.builder()
            .id("14285796") //18864
            .login("podyachikh@kia.agatauto.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_EKB = () -> Account.builder()
            .id("24713246") //31262
            .login("ekb@brightpark.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_1_6_CITIES = () -> Account.builder()
            .id("21236408") //25490
            .login("avtovor2@freshauto.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_7_CITIES = () -> Account.builder()
            .id("16165433") //21011
            .login("simferopol_auto@mail.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_7_CITIES = () -> Account.builder()
            .id("33427392") //26626
            .login("tsuldashev@automir-dv.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_1_6_CITIES = () -> Account.builder()
            .id("17221837") //22087
            .login("atc.belgorod@yandex.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_DIRECT_USER_FROM_EKB = () -> Account.builder()
            .id("22863796") //26774
            .login("maserati@maserati-ural.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> NEW_CABINET_AGENCY_USER_FROM_MOSCOW = () -> Account.builder()
            .id("10380477")
            .login("ysolovev@autocentercity.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();


    public static final Provider<Account> CABINET_DIRECT_USER_FROM_REG_1 = () -> Account.builder()
            .id("28371472")
            .login("avto.price@bk.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> CABINET_USER_FOR_CALLS = () -> Account.builder()
            .id("10703195")
            .login("mihail.lezhov@avilon.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> START_PAGE_DIRECT_DEALER = () -> Account.builder()
            .id("8371466") //6434
            .login("poryvakin.ov@sberleasing.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

    public static final Provider<Account> START_PAGE_AGENCY_DEALER = () -> Account.builder()
            .id("19655395") //7806
            .login("favorit.motors.mkad@yandex.ru")
            .password("autoru")
            .phone(Optional.of(Utils.getRandomPhone())).build();

}
