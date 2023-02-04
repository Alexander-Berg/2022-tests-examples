package ru.yandex.general.consts;

import lombok.Getter;

public class BaseConstants {

    private BaseConstants() {}

    @Getter
    public enum ClientType {

        DESKTOP("desktop"),
        TOUCH("touch");

        private String type;

        ClientType(String type) {
            this.type = type;
        }
    }

    @Getter
    public enum ListingType {

        GRID("grid"),
        LIST("list");

        private String type;

        ListingType(String type) {
            this.type = type;
        }
    }

    @Getter
    public enum Condition {

        NEW("New"),
        USED("Used");

        private String condition;

        Condition(String condition) {
            this.condition = condition;
        }
    }

    @Getter
    public enum AddressType {

        ADDRESS("Address"),
        DISTRICT("District"),
        METRO_STATION("MetroStation");

        private String addressType;

        AddressType(String addressType) {
            this.addressType = addressType;
        }
    }

}
