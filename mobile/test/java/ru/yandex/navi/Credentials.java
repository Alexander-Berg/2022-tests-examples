package ru.yandex.navi;

public class Credentials {
    public final String userName;
    public final String password;

    public Credentials(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public static final Credentials AUTO_TEST_NAVI = new Credentials("auto.test.navi", "qaswedfr");
    public static final Credentials PARKING = new Credentials("parking.test", "APt-TZ5-8yH-J9a");
}
