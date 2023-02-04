package ru.yandex.general.beans.ldJson;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class ProductOffer {

    @SerializedName("@type")
    String type;
    String name;
    String description;
    Brand brand;
    Offers offers;
    List<String> image;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static ProductOffer productOffer() {
        return new ProductOffer().setType("Product");
    }

}
