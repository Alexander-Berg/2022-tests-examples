package ru.yandex.general.beans.metrics;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Product {

    String id;
    String name;
    Object price;
    String stringPrice;
    String category;

    public static Product product() {
        return new Product();
    }

}
