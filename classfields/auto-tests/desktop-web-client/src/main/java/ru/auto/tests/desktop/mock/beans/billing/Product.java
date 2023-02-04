package ru.auto.tests.desktop.mock.beans.billing;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Product {

    String name;
    Integer count;

    public static Product product(String name) {
        return new Product().setName(name).setCount(1);
    }

}
