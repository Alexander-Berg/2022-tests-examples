package ru.auto.tests.passport.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;

public class CreateAccountResp {

    @Getter
    @NonNull
    @SerializedName("uid")
    private String uid;
}
