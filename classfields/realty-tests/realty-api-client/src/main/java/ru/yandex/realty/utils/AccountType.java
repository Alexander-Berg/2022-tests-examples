package ru.yandex.realty.utils;

/**
 * Created by vicdev on 10.04.17.
 */
public enum AccountType {
    OWNER(0),
    AGENT(1),
//     не используется
    DEVELOPER(2),
    AGENCY(3);

    long type;

    AccountType(long type) {
        this.type = type;
    }

    public long getValue() {
        return type;
    }
}
