package ru.auto.tests.passport.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;

public class Captcha {

    @Getter
    @NonNull
    @SerializedName("answer")
    private String answer;
}
