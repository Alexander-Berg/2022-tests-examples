package ru.auto.tests.desktop.consts;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;

import java.util.Map;

import static ru.auto.tests.desktop.consts.CarGearType.gearType;
import static ru.auto.tests.desktop.consts.CarTransmissionType.transmission;

/**
 * @author kurau (Yuri Kalinin)
 */
public class Cars {

    @Getter
    private final static Map<String, String> modifications = ImmutableMap.<String, String>builder()
            .put("ALLROAD", "Внедорожник")
            .put("ALLROAD_3_DOORS", "Внедорожник 3 дв.")
            .put("ALLROAD_5_DOORS", "Внедорожник 5 дв.")
            .put("ALLROAD_OPEN", "Внедорожник открытый")
            .put("CABRIO", "Кабриолет")
            .put("COMPACTVAN", "Компактвэн")
            .put("COUPE", "Купе")
            .put("COUPE_HARDTOP", "Купе-хардтоп")
            .put("LANDO", "Ландо")
            .put("LIMOUSINE", "Лимузин")
            .put("LIFTBACK", "Лифтбек")
            .put("MICROVAN", "Микровэн")
            .put("MINIVAN", "Минивэн")
            .put("PICKUP", "Пикап")
            .put("PICKUP_TWO", "Пикап Двойная кабина")
            .put("PICKUP_ONE", "Пикап Одинарная кабина")
            .put("PICKUP_ONE_HALF", "Пикап Полуторная кабина")
            .put("ROADSTER", "Родстер")
            .put("SEDAN", "Седан")
            .put("SEDAN_2_DOORS", "Седан 2 дв.")
            .put("SEDAN_HARDTOP", "Седан-хардтоп")
            .put("SPEEDSTER", "Спидстер")
            .put("TARGA", "Тарга")
            .put("WAGON", "Универсал")
            .put("WAGON_3_DOORS", "Универсал 3 дв.")
            .put("WAGON_5_DOORS", "Универсал 5 дв.")
            .put("FASTBACK", "Фастбек")
            .put("PHAETON", "Фаэтон")
            .put("PHAETON_WAGON", "Фаэтон-универсал")
            .put("VAN", "Фургон")
            .put("HATCHBACK_3_DOORS", "Хэтчбек 3 дв.")
            .put("HATCHBACK_4_DOORS", "Хэтчбек 4 дв.")
            .put("HATCHBACK_5_DOORS", "Хэтчбек 5 дв.")
            .put("CABRIOLET", "Кабриолет")
            .put("CROSSOVER", "Кроссовер")
            .put("MINIBUS", "Микроавтобус")
            .put("MINI_WEN", "Минивэн")
            .put("PICK_UP", "Пикап")
            .put("UNIVERSAL", "Универсал")
            .put("HATCHBACK", "Хэтчбек")
            .build();

    @Getter
    private final static Map<String, CarEngineType> engines = ImmutableMap.<String, CarEngineType>builder()
            .put("H2", new CarEngineType().setName("гидроген").setShortName("h2")
                    .setAdjective("гидрогенный").setAdjectivePlural("гидрогенные"))
            .put("GASOLINE", new CarEngineType().setName("бензин").setAdjective("бензиновый").setAdjectivePlural("бензиновые"))
            .put("DIESEL", new CarEngineType().setName("дизель").setShortName("d")
                    .setAdjective("дизельный").setAdjectivePlural("дизельные"))
            .put("HYBRID", new CarEngineType().setName("гибрид").setShortName("h").
                    setAdjective("гибридный").setAdjectivePlural("гибридные"))
            .put("LPG", new CarEngineType().setName("СУГ").setShortName("lpg").setAdjective("СУГ").setAdjectivePlural("СУГ"))
            .put("ELECTRO", new CarEngineType().setName("электро").setAdjective("электро").setAdjectivePlural("электро"))
            .build();

    @Getter
    private final static Map<String, CarGearType> gears = ImmutableMap.<String, CarGearType>builder()
            .put("ALL_WHEEL_DRIVE", gearType().setName("полный").setShortName("4x4"))
            .put("FORWARD_CONTROL", gearType().setName("передний").setShortName("FWD"))
            .put("REAR_DRIVE", gearType().setName("RWD").setShortName("задний"))
            .build();

    @Getter
    private final static Map<String, CarTransmissionType> transmissions = ImmutableMap.<String, CarTransmissionType>builder()
            .put("ROBOT", transmission().setShortName("AMT").setName("робот").setFullName("Робот"))
            .put("ROBOTIC", transmission().setShortName("AMT").setName("робот").setFullName("Робот"))
            .put("AUTOMATIC", transmission().setShortName("AT").setName("автомат").setFullName("Автоматическая"))
            .put("VARIATOR", transmission().setShortName("CVT").setName("вариатор").setFullName("Вариатор"))
            .put("MECHANICAL", transmission().setShortName("MT").setName("механика").setFullName("Механическая"))
            .put("SEMI_AUTOMATIC", transmission().setShortName("").setName("полуавтомат").setFullName("Полуавтоматическая"))
            .put("TIPTRONIC", transmission().setShortName("AT").setName("типтроник").setFullName("Типтроник"))
            .build();

}
