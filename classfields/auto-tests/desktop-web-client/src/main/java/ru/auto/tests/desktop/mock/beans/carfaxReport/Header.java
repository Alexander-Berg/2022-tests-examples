package ru.auto.tests.desktop.mock.beans.carfaxReport;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Header {

    String title;
    @SerializedName("timestamp_update")
    String timestampUpdate;

    public static Header header() {
        return new Header();
    }

}
