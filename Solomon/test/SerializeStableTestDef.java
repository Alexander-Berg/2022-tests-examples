package ru.yandex.stockpile.ser.test;

import ru.yandex.solomon.codec.serializer.StockpileFormat;

/**
 * @author Stepan Koltsov
 */
public class SerializeStableTestDef {

    public final String base;
    public final SerializeStableContext<?, ?> serializeStableContext;
    public final StockpileFormat firstAvailableFormat;

    public SerializeStableTestDef(String base, SerializeStableContext<?, ?> serializeStableContext, StockpileFormat firstAvailableFormat) {
        this.base = base;
        this.serializeStableContext = serializeStableContext;
        this.firstAvailableFormat = firstAvailableFormat;
    }
}
