package ru.yandex.realty.misc.enums;

/**
 * For IntEnumResolver testing
 *
 * @author Ildar Safarov
 */
public enum Qualities implements IntEnum {
    UNKNOWN(0),
    CRUELTY(1),
    KINDNESS(2),

    TENDERNESS(5),
    EGOISM(10),
    INDIFFERENCE(12),
    GENEROSITY(13),
    ;

    private final int value;

    Qualities(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static IntEnumResolver<Qualities> R = IntEnumResolver.r(Qualities.class);
}
