package ru.yandex.realty.core.xbi;

import java.util.List;

import static ru.yandex.common.util.collections.CollectionFactory.newArrayList;

/**
 * @author aherman
 *
 */
public class Simple {
    String field1;
    int field2;
    float field3;
    double field4;
    List<String> field5 = newArrayList();
    A field6;

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public void setField2(int field2) {
        this.field2 = field2;
    }

    public void setField3(float field3) {
        this.field3 = field3;
    }

    public void setField4(double field4) {
        this.field4 = field4;
    }

    public void setField5(String field5) {
        this.field5.add(field5);
    }

    public void setField6(A field6) {
        this.field6 = field6;
    }
}
