package ru.yandex.realty.beans.juridical;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
public class UserContacts {

    @Getter
    @Setter
    @SerializedName("name")
    private String name;

    @Getter
    @Setter
    @SerializedName("email")
    private String email;

    @Getter
    @Setter
    @SerializedName("phones")
    private List<Phone> phones;

    @Getter
    @Setter
    @SerializedName("ogrn")
    private String ogrn;
}
