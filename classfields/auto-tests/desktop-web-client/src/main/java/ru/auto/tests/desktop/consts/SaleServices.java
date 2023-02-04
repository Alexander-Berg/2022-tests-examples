package ru.auto.tests.desktop.consts;

import lombok.Getter;

public class SaleServices {

    private SaleServices() {}

    @Getter
    public enum VasProduct {

        PACKAGE_VIP("Пакет VIP", "package_vip"),
        PACKAGE_TURBO("Турбо продажа", "package_turbo"),
        PACKAGE_EXPRESS("Экспресс продажа", "package_express"),
        ALL_SALE_TOPLIST("Поднятие в ТОП", "all_sale_toplist"),
        ALL_SALE_ACTIVATE("Активация объявления", "all_sale_activate"),
        ALL_SALE_COLORS("Выделение цветом", "all_sale_color"),
        ALL_SALE_FRESH("Поднятие в поиске", "all_sale_fresh"),
        ALL_SALE_SPECIAL("Спецпредложение", "all_sale_special"),
        SHOW_IN_STORIES("Показ в Историях", "show-in-stories");

        private final String name;
        private final String value;

        VasProduct(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
