package ru.auto.tests.desktop.mock.beans.auctionState;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Context {

    @SerializedName("mark_code")
    String markCode;
    @SerializedName("mark_ru")
    String markRu;
    @SerializedName("mark_name")
    String markName;
    @SerializedName("model_code")
    String modelCode;
    @SerializedName("model_ru")
    String modelRu;
    @SerializedName("model_name")
    String modelName;
    @SerializedName("region_id")
    String regionId;

    public static Context context() {
        return new Context();
    }

}
