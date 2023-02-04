package ru.yandex.general.beans.tus;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class CreateAccountResponse {

    String status;
    boolean saved;
    AccountResponse account;

    @SerializedName("passport_environment")
    String passportEnvironment;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
