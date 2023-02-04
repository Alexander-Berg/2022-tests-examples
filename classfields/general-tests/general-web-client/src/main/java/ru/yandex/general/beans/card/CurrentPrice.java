package ru.yandex.general.beans.card;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CurrentPrice {

    @SerializedName("__typename")
    String typename;
    String priceRur;
    String salaryRur;

    public static CurrentPrice currentPrice() {
        return new CurrentPrice();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
