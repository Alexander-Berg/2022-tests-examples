package ru.auto.tests.passport.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

public class Blackbox {

    @Getter
    @NonNull
    @SerializedName("users")
    private List<User> users;

    public class User {

        @Getter
        @NonNull
        @SerializedName("id")
        private String id;

        @Getter
        @NonNull
        @SerializedName("login")
        private String login;
    }
}
