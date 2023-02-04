package ru.auto.tests.passport.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;

public class ConfirmAndBindSecureSubmitResp {

    @Getter
    @NonNull
    @SerializedName("track_id")
    private String trackId;
}
