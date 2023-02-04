package ru.auto.tests.desktop.mock.beans.offer.seller;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Phones {

    String phone;
    @SerializedName("call_hour_start")
    int callHoursStart;
    @SerializedName("call_hour_end")
    int callHoursEnd;
    String original;

    public static Phones phones() {return new Phones();}

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);}
}
