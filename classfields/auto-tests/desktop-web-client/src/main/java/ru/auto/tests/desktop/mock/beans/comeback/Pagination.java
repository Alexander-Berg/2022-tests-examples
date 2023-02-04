package ru.auto.tests.desktop.mock.beans.comeback;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Pagination {

    int page;
    @SerializedName("page_size")
    int pageSize;

    public static Pagination pagination() {
        return new Pagination();
    }

}
