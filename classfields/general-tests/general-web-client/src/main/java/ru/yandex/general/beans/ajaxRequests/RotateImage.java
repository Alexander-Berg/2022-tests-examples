package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class RotateImage {

    int angle;
    int groupId;
    String name;
    String namespace;
    int ratio;
    @SerializedName("size_260x146")
    String size260x146;
    @SerializedName("size_260x146_2x")
    String size260x146x2;
    @SerializedName("size_260x194")
    String size260x194;
    @SerializedName("size_260x194_2x")
    String size260x194x2;
    @SerializedName("size_260x260")
    String size260x260;
    @SerializedName("size_260x260_2x")
    String size260x260x2;
    @SerializedName("size_260x346")
    String size260x346;
    @SerializedName("size_260x346_2x")
    String size260x346x2;

    public static RotateImage rotateImage() {
        return new RotateImage();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
