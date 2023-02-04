package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class HiringOrganization {

    @SerializedName("@type")
    String type;
    String name;

    public static HiringOrganization hiringOrganization(String name) {
        return new HiringOrganization().setType("Organization").setName(name);
    }

}
