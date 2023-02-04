package ru.yandex.general.consts;

import lombok.Getter;

public class FormConstants {

    private FormConstants() {
    }

    @Getter
    public enum Categories {

        PERENOSKA("Переноска", "Транспортировка, переноски", "Животные и товары для них", "/transportirovka-perenoski/", false, false, "transportirovka-perenoski_2HHoms"),
        UMNIE_KOLONKI("Умная колонка", "Умные колонки", "Электроника", "/umnie-kolonki/", false, false, "umnie-kolonki_mQdWwr"),
        AKUSTICHESKIE_SISTEMI("Акустические система", "Акустические системы", "Электроника", "/akusticheskie-sistemi/", false, false, "akusticheskie-sistemi_h1HFsW"),
        REZUME_IT("Резюме IT, интернет и реклама", "Поиск работы в IT, интернет, связь, телеком", "Услуги", "/rezume-it-internet-svyaz-telekom/", true, true, "it-internet-svyaz-telekom_sbCJHA"),
        VAKANCIYA_RABOCHII("Вакансия рабочий", "Вакансии рабочего", "Работа", "/vakansii-proizvodstvo-rabochiy/", true, true, "vakansii-proizvodstvo-rabochiy_TTiLwY"),
        VAKANCIYA_MENEGER("Вакансия менеджер без опыта", "Вакансии менеджера без опыта", "Работа", "/vakansii-bez-specialnoy-podgotovki-menedzher/", true, true, ""),
        DRUGOI_GOTOVIY_BIZNES("Другой готовый бизнес", "Другой готовый бизнес", "Бизнес и оборудование", "/drugoy-gotovyy-biznes/", false, true, ""),
        REZUME_DESIGN("Дизайн", "Дизайн и изготовление товаров", "Работа", "/dizayn-i-izgotovlenie-tovarov/", true, true, ""),
        SADOVII_INVENTAR("Садовый инвентарь", "Садовый инвентарь и инструменты", "Товары для дачи, сада и огорода", "", false, false, ""),
        REZUME_IN_SELLING("Резюме работа в продажах", "Резюме в продажах", "Работа", "/rezume-prodazhi/", true, true, ""),
        USLUGI_DOSTAVKI("Услуга доставки", "Услуги доставки и грузоперевозок", "Услуги", "/uslugi-kurerskoy-dostavki-i-gruzoperevozok/", true, true, "kurery-i-gruzoperevozki_ymIE6O");

        private String title;
        private String categoryName;
        private String parentCategory;
        private String categoryPath;
        private boolean workCategory;
        private boolean skipCondition;
        private String categoryId;

        Categories(String title, String categoryName, String parentCategory, String categoryPath, boolean workCategory, boolean skipCondition, String categoryId) {
            this.title = title;
            this.categoryName = categoryName;
            this.parentCategory = parentCategory;
            this.categoryPath = categoryPath;
            this.workCategory = workCategory;
            this.skipCondition = skipCondition;
            this.categoryId = categoryId;
        }
    }

    @Getter
    public enum AttributeTypes {

        MULTISELECT("Мультиселект"),
        SELECT("Селект"),
        SWITCHER("Свитчер"),
        INPUT("Инпут");

        private String type;

        AttributeTypes(String type) {
            this.type = type;
        }
    }

    @Getter
    public enum Conditions {

        NEW("Новый товар", "New"),
        USED("Уже использовался", "Used");

        private String condition;
        private String value;

        Conditions(String condition, String value) {
            this.condition = condition;
            this.value = value;
        }
    }

}
