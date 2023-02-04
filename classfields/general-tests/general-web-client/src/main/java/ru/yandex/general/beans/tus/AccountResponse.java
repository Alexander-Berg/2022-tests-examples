package ru.yandex.general.beans.tus;

import lombok.Getter;

@Getter
public class AccountResponse {

    String login;
    String password;
    String firstname;
    String lastname;
    String language;
    String country;
    String delete_at;
    String uid;

}
