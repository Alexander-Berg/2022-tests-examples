package ru.yandex.general.beans.wizard;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class AutoItem {

    String image;
    @SerializedName("image_retina")
    String imageRetina;
    String name;
    String url;
    String text;

    public static AutoItem autoItem() {
        return new AutoItem();
    }

}
