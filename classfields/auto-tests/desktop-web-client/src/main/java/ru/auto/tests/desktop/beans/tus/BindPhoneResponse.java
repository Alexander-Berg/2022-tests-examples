package ru.auto.tests.desktop.beans.tus;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class BindPhoneResponse {

    @SerializedName("passport_environment")
    String passportEnvironment;
    @SerializedName("phone_number")
    String phoneNumber;
    String status;

}
