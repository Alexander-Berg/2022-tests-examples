package ru.yandex.general.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.consts.FormConstants.AttributeTypes;

import java.util.List;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.FormConstants.AttributeTypes.INPUT;
import static ru.yandex.general.consts.FormConstants.AttributeTypes.MULTISELECT;
import static ru.yandex.general.consts.FormConstants.AttributeTypes.SELECT;
import static ru.yandex.general.consts.FormConstants.AttributeTypes.SWITCHER;

@Getter
@Setter
@Accessors(chain = true)
public class Attribute {

    AttributeTypes attributeType;
    String name;
    List<String> values;
    String value;

    public static Attribute multiselect(String name) {
        return new Attribute().setName(name).setAttributeType(MULTISELECT);
    }

    public static Attribute select(String name) {
        return new Attribute().setName(name).setAttributeType(SELECT);
    }

    public static Attribute switcher(String name) {
        return new Attribute().setName(name).setAttributeType(SWITCHER);
    }

    public static Attribute input(String name) {
        return new Attribute().setName(name).setAttributeType(INPUT);
    }

    public Attribute setValues(String... values) {
        this.values = asList(values);
        return this;
    }

}
