package ru.auto.tests.desktop.mock.beans.photos;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Sizes {

    String full;
    String small;
    String orig;
    @SerializedName("thumb_m")
    String thumbM;
    @SerializedName("832x624")
    String s832x624;
    @SerializedName("456x342n")
    String s456x342n;
    @SerializedName("320x240")
    String s320x240;
    @SerializedName("1200x900")
    String s1200x900;
    @SerializedName("120x90")
    String s120x90;
    @SerializedName("92x69")
    String s92x69;
    @SerializedName("456x342")
    String s456x342;
    @SerializedName("1200x900n")
    String s1200x900n;

    public static Sizes sizes() {
        return new Sizes();
    }

}
