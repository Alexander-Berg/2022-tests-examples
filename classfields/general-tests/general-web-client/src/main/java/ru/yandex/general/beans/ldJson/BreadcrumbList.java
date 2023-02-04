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
public class BreadcrumbList {

    @SerializedName("@type")
    String type;
    List<BreadcrumbItem> itemListElement;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static BreadcrumbList breadcrumbList(BreadcrumbItem... breadcrumbItems) {
        return new BreadcrumbList().setType("BreadcrumbList").setItemListElement(asList(breadcrumbItems));
    }

}
