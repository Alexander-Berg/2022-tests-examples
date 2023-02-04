package ru.yandex.realty.beans.juridical;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class JuridicalUserBody {

    @Getter
    @Setter
    @SerializedName("userInfo")
    private UserInfo userInfo;

    @Getter
    @Setter
    @SerializedName("userContacts")
    private UserContacts userContacts;

    @Getter
    @Setter
    @SerializedName("userSettings")
    private UserSettings userSettings;

    public static JuridicalUserBody juridicalUserBody() {
        return new JuridicalUserBody();
    }
}
