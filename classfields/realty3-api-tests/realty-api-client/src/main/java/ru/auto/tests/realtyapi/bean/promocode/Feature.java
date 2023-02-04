package ru.auto.tests.realtyapi.bean.promocode;

import lombok.Data;
import lombok.experimental.Accessors;

import static java.lang.String.format;

@Data
@Accessors(chain = true)
public class Feature {
    private String tag;
    private String lifetime;
    private int count;
    private String payload;

    private static final int ONE_DAY = 1;
    private static final int VALID_RGID = 143;

    public Feature(String tag, int count) {
        this.setTag(tag)
                .setValidLifetime()
                .setValidPayload()
                .setCount(count);
    }

    private Feature setValidLifetime() {
        this.lifetime = format("%d days", ONE_DAY);
        return this;
    }

    private Feature setValidPayload() {
        this.payload = format("region_id=%s", VALID_RGID);
        return this;
    }
}
