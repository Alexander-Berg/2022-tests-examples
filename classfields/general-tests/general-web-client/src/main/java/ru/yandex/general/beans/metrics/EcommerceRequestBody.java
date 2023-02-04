package ru.yandex.general.beans.metrics;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class EcommerceRequestBody {

    @SerializedName("__ym")
    Ym ym;

    public static EcommerceRequestBody ecommerceRequestBody() {
        return new EcommerceRequestBody();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
