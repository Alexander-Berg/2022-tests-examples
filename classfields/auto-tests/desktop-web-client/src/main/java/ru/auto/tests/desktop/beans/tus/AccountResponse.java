package ru.auto.tests.desktop.beans.tus;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class AccountResponse {

    String login;
    String password;
    String firstname;
    String lastname;
    String language;
    String country;
    @SerializedName("delete_at")
    String deleteAt;
    String uid;

}
