package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class BreadcrumbItem {

    @SerializedName("@type")
    String type;
    int position;
    String name;
    String item;

    public static BreadcrumbItem breadcrumbItem() {
        return new BreadcrumbItem().setType("ListItem");
    }

}
