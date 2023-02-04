package ru.auto.tests.passport.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;

public class Track {

    @Getter
    @NonNull
    @SerializedName("id")
    private String id;

    @Getter
    @NonNull
    @SerializedName("phone_confirmation_code")
    private String phoneConfirmationCode;
}
