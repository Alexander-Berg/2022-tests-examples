package ru.auto.tests.desktop.mock.beans.carfaxReport;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class VehiclePhotos {

    Header header;
    List<Record> records;
    String status;
    @SerializedName("record_count")
    Integer recordCount;

    public static VehiclePhotos vehiclePhotos() {
        return new VehiclePhotos();
    }

}
