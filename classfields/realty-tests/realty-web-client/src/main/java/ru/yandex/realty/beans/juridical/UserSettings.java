package ru.yandex.realty.beans.juridical;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class UserSettings {

    @Getter
    @Setter
    @SerializedName("redirectPhones")
    private Boolean redirectPhones;
}
