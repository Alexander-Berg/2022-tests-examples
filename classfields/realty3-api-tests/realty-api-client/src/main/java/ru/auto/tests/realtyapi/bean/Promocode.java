package ru.auto.tests.realtyapi.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.auto.tests.realtyapi.bean.promocode.Constraints;
import ru.auto.tests.realtyapi.bean.promocode.Feature;

import static java.lang.String.format;

@Data
@Accessors(chain = true)
public class Promocode {
    private String code;
    private Feature[] features;
    private Constraints constraints;
    private String[] aliases;

    public Promocode() {
        long promocodeId = System.currentTimeMillis();

        this.code = format("auto-test-%d", promocodeId);
        this.aliases = new String[]{(format("auto-test-alias-%d", promocodeId))};
    }

    public Promocode setOneFeature(Feature feature) {
        this.features = new Feature[]{feature};
        return this;
    }

    public String  getOneAlias() {
        return aliases[0];
    }
}
