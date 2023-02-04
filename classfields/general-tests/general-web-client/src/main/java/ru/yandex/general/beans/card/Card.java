package ru.yandex.general.beans.card;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Card {

    String id;
    String title;
    String createDateTime;
    List<PurchasableProduct> purchasableProducts;
    Category category;
    String description;
    List<Photo> photos;
    Video video;
    List<Attribute> attributes;
    Price price;
    boolean delivery;
    Contacts contacts;
    boolean favorite;
    Seller seller;
    String note;
    String condition;
    DeliveryInfo deliveryInfo;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
