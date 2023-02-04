package ru.auto.tests.desktop.models;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Singleton;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Data
@Accessors(chain = true)
@Singleton
public class Offer {

    private String mark = "Audi";
    private String model = "TT";
    private String year = "2010";
    private String bodyType = "Купе";
    private String generation = "II (8J) Рестайлинг";
    private String engineType = "Бензин";
    private String gearType = "Передний";
    private String transmission = "Робот";
    private String techParam = "211\u00a0л.с. (2.0 AMT)";
    private String color = "FAFBFB";
    private String mileage = "100";
    private String video = "https://www.youtube.com/watch?v=qQHEtfNY_cU";
    private String photo = "audi_a3.jpeg";
    private String ptsType = "Оригинал / Электронный ПТС";
    private String ownersCount = "Первый";
    private String purchaseYear = "2020";
    private String purchaseMonth = "Август";
    private String warrantyYear = "2030";
    private String warrantyMonth = "Август";
    private String description = "Супер-тачка! Когда с НДС";
    private String contactName = "Тест";
    private String contactEmail = "test@test.org";
    private String communicationType = "Только в чате";
    private String address = "Покровка, 32";
    private String price = "500000";
    private String plateNumber = "x123xx 54";
    private String vin = "FLLGD18508S219366";
    private String sts = "6360269398";
    private String complectation = "Luxe";
    private List<String> checkboxOptions = newArrayList(
            "Антиблокировочная система (ABS)",
            "Система доступа без ключа",
            "AUX",
            "USB"
    );
    private Pair<String, String> selectOption = Pair.of("Фары", "Лазерные");
    private Boolean gbo = true;
    private Boolean beaten = true;
    private Boolean custom = true;
    private Boolean onlineView = true;
    private Boolean exchange = true;
}
