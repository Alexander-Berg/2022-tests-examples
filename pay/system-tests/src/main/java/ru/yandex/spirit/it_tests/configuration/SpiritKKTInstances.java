package ru.yandex.spirit.it_tests.configuration;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.UUID;

@UtilityClass
public class SpiritKKTInstances {
    public static List<SpiritKKT> AllKKTS = List.of(
            new SpiritKKT(
                    "kkt1", "10000000010000001111", "9999000000000001", "100051000501"
            ),
            new SpiritKKT(
                    "kkt2", "10000000010000002222", "9999000000000002", "7704340310"
            ),
            new SpiritKKT(
                    "kkt3", "10000000010000003333", "9999000000000003", "7704340310"
            )
    );

    public static String generateFnSerial() {
        return "9999" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 12);
    }
}
