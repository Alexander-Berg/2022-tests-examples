package ru.yandex.general.beans.ldJson;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Arrays.asList;

@Setter
@Getter
@Accessors(chain = true)
public class Organization {

    @SerializedName("@type")
    String type;
    String name;
    String url;
    List<String> sameAs;
    Address address;
    String telephone;
    String logo;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static Organization organization() {
        return new Organization().setType("Organization");
    }

}
