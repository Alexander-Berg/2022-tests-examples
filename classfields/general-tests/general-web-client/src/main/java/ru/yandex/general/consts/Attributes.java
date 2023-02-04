package ru.yandex.general.consts;

import lombok.Getter;
import ru.yandex.general.beans.card.Attribute;

import static ru.yandex.general.beans.card.Attribute.attribute;
import static ru.yandex.general.beans.card.AttributeValue.value;
import static ru.yandex.general.consts.Attributes.Typename.BOOLEAN;
import static ru.yandex.general.consts.Attributes.Typename.INPUT;
import static ru.yandex.general.consts.Attributes.Typename.MULTISELECT;
import static ru.yandex.general.consts.Attributes.Typename.SELECT;


public class Attributes {

    private Attributes() {
    }

    public static Attribute summaryPower() {
        return createAttribute(INPUT).setId("summarnaya-moschnost_15558796_H7M8y9")
                .setName("Суммарная мощность")
                .setMetric("Вт");
    }

    public static Attribute workTime() {
        return createAttribute(INPUT).setId("vremya-raboti_0_7nK1u9")
                .setName("Время работы")
                .setMetric("час");
    }

    public static Attribute builtInScreen() {
        return createAttribute(BOOLEAN).setId("vstroenniy-ekran_jDZ2bD")
                .setName("Встроенный экран");
    }

    public static Attribute bluetooth() {
        return createAttribute(BOOLEAN).setId("bluetooth_xz35Pm")
                .setName("Bluetooth");
    }

    public static Attribute powerSupplyType() {
        return createAttribute(MULTISELECT).setId("tip-pitaniya_8_hCo8gj")
                .setName("Тип питания");
    }

    public static Attribute driverLicenseCategory() {
        return createAttribute(MULTISELECT).setId("kategoriya-prav-dlya-rezyume_OTu0nW")
                .setName("Категория прав");
    }

    public static Attribute manufacturer() {
        return createAttribute(SELECT).setId("proizvoditel-umnyh-kolonok_Njqq1d")
                .setName("Производитель ");
    }

    public static Attribute voiceAssistant() {
        return createAttribute(SELECT).setId("golosovoy-pomoschnik_15561008_T1P3i0")
                .setName("Голосовой помощник  ");
    }

    public static Attribute workSchedule() {
        return createAttribute(SELECT).setId("grafik-raboty_qZCuFX")
                .setName("График работы");
    }

    public static Attribute education() {
        return createAttribute(SELECT).setId("obrazovanie-dlya-raboty_DcO3m0")
                .setName("Образование");
    }

    public static Attribute workExpirence() {
        return createAttribute(SELECT).setId("opyt-raboty_uMISRI")
                .setName("Опыт работы");
    }

    public static Attribute sex() {
        return createAttribute(SELECT).setId("pol-v-rezyume_O7A8gX")
                .setName("Пол");
    }

    public static Attribute typeOfEmployment() {
        return createAttribute(SELECT).setId("tip-zanyatosti_l55Uem")
                .setName("Тип занятости");
    }

    public static Attribute age() {
        return createAttribute(INPUT).setId("vozrast-v-rezyume_6F98jk")
                .setName("Возраст");
    }

    public static Attribute createAttribute(Typename value) {
        return attribute().setDescription(null)
                .setMetric(null)
                .setValue(value().setTypename(value.getValue()));
    }

    @Getter
    public enum Typename {

        INPUT("CardAttributeNumberValue"),
        BOOLEAN("CardAttributeBooleanValue"),
        SELECT("CardAttributeDictionaryValue"),
        MULTISELECT("CardAttributeRepeatedDictionaryValue");

        private String value;

        Typename(String value) {
            this.value = value;
        }
    }

}
