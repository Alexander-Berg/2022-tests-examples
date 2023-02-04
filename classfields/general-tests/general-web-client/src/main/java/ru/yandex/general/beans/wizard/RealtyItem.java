package ru.yandex.general.beans.wizard;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class RealtyItem {

    @SerializedName("image_prefix")
    String imagePrefix;
    Price price;
    Metro metro;
    String name;
    String description;
    int offerImagesCount;
    String text;
    String type;
    String url;
    List<String> gallery;

    public static RealtyItem realtyItem() {
        return new RealtyItem();
    }

}
