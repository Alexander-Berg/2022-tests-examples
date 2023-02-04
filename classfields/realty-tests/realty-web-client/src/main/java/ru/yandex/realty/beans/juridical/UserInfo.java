package ru.yandex.realty.beans.juridical;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class UserInfo {

    @Getter
    @Setter
    @SerializedName("userSource")
    private String userSource;

    @Getter
    @Setter
    @SerializedName("paymentType")
    private String paymentType;

    @Getter
    @Setter
    @SerializedName("userType")
    private String userType;
}
