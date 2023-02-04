package ru.auto.tests.desktop.mock.beans.photos;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Photo {

    String name;
    Sizes sizes;
    Transform transform;
    Preview preview;
    String namespace;
    @SerializedName("photo_class")
    String photoClass;
    @SerializedName("create_date")
    String createDate;
    @SerializedName("orig_width")
    Integer origWidth;
    @SerializedName("orig_height")
    Integer origHeight;

    public static Photo photo() {
        return new Photo();
    }

}
