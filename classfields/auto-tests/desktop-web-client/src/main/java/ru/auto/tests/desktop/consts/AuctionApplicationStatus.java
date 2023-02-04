package ru.auto.tests.desktop.consts;

import lombok.Getter;

public class AuctionApplicationStatus {

    public static String VIEW = "Осмотр";
    public static String PRICE = "Цена";
    public static String PRICE_OFFER = "Предложение цены";
    public static String DEALS = "Сделка";

    private AuctionApplicationStatus() {
    }

    @Getter
    public enum StatusName {

        NEW("NEW"),
        DEAL("DEAL"),
        INSPECTED("INSPECTED"),
        AUCTION("AUCTION"),
        REJECTED("REJECTED"),
        FINISHED("FINISHED");

        private final String status;

        StatusName(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

}
